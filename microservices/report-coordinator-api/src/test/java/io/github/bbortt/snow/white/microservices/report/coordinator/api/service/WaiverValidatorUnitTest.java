/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.Waiver;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WaiverValidatorUnitTest {

  private static final LocalDate EXPIRES = LocalDate.parse("2026-12-31");

  private static final ApiTest A_NEW_HOPE_V1 = ApiTest.builder()
    .serviceName("starWars")
    .apiName("aNewHope")
    .apiVersion("1")
    .apiType(OPENAPI.getVal())
    .build();

  private static final ApiTest A_NEW_HOPE_V2 = ApiTest.builder()
    .serviceName("starWars")
    .apiName("aNewHope")
    .apiVersion("2")
    .apiType(OPENAPI.getVal())
    .build();

  private WaiverValidator fixture;

  @BeforeEach
  void beforeEach() {
    fixture = new WaiverValidator();
  }

  private static Waiver.WaiverBuilder acceptableWaiver() {
    return Waiver.builder()
      .serviceName("starWars")
      .apiName("aNewHope")
      .apiVersion("1")
      .path("/x-wings")
      .method("GET")
      .reason("Known gap, tracked elsewhere.")
      .expires(EXPIRES);
  }

  @Nested
  class ValidateTest {

    @Test
    void shouldAcceptAnEmptyDocument() {
      assertThat(fixture.validate(List.of(), Set.of(A_NEW_HOPE_V1))).isEmpty();
    }

    @Test
    void shouldAcceptAWaiverNamingAnApiOfTheReport() {
      assertThat(
        fixture.validate(
          List.of(acceptableWaiver().build()),
          Set.of(A_NEW_HOPE_V1)
        )
      ).isEmpty();
    }

    @Test
    void shouldAcceptEachTargetDiscriminatorOnItsOwn() {
      var waivers = List.of(
        acceptableWaiver().responseCode("418").build(),
        acceptableWaiver().parameterName("page").build(),
        acceptableWaiver().contentType("application/json").build()
      );

      assertThat(fixture.validate(waivers, Set.of(A_NEW_HOPE_V1))).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
    void shouldRejectABlankReason() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().reason("   ").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).containsExactly(
        "Waiver { serviceName='starWars', apiName='aNewHope', apiVersion='1', path='/x-wings', method='GET' } carries a blank reason!"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
    void shouldRejectSeveralTargetDiscriminators() {
      var rejections = fixture.validate(
        List.of(
          acceptableWaiver().responseCode("418").parameterName("page").build()
        ),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).containsExactly(
        "Waiver { serviceName='starWars', apiName='aNewHope', apiVersion='1', path='/x-wings', method='GET' } names several target discriminators - at most one of responseCode, parameterName and contentType!"
      );
    }

    @Test
    void shouldRejectAllThreeTargetDiscriminatorsAtOnceAsOneComplaint() {
      var rejections = fixture.validate(
        List.of(
          acceptableWaiver()
            .responseCode("418")
            .parameterName("page")
            .contentType("application/json")
            .build()
        ),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(1);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
    void shouldRejectAWaiverMatchingNoApiTestOfTheReport() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().apiName("theCloneWars").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).containsExactly(
        "Waiver { serviceName='starWars', apiName='theCloneWars', apiVersion='1', path='/x-wings', method='GET' } matches no API test of this report!"
      );
    }

    @Test
    void shouldRejectAWaiverNamingAVersionTheReportDoesNotJudge() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().apiVersion("3").build()),
        Set.of(A_NEW_HOPE_V1, A_NEW_HOPE_V2)
      );

      assertThat(rejections).hasSize(1);
    }

    @Test
    void shouldRejectAWaiverNamingAServiceTheReportDoesNotJudge() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().serviceName("starTrek").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(1);
    }

    @Test
    void shouldAcceptAWildcardedVersionAgainstEveryVersionOfThatApi() {
      assertThat(
        fixture.validate(
          List.of(acceptableWaiver().apiVersion("*").build()),
          Set.of(A_NEW_HOPE_V1, A_NEW_HOPE_V2)
        )
      ).isEmpty();
    }

    @Test
    void shouldRejectAWildcardedVersionForAnApiTheReportDoesNotJudge() {
      var rejections = fixture.validate(
        List.of(
          acceptableWaiver().apiName("theCloneWars").apiVersion("*").build()
        ),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(1);
    }

    @Test
    void shouldCollectEveryRejectionOfEveryEntryInDocumentOrder() {
      var rejections = fixture.validate(
        List.of(
          acceptableWaiver().reason(" ").build(),
          acceptableWaiver().apiName("theCloneWars").build(),
          acceptableWaiver().build()
        ),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections)
        .hasSize(2)
        .satisfiesExactly(
          first -> assertThat(first).contains("blank reason"),
          second -> assertThat(second).contains("matches no API test")
        );
    }

    @Test
    void shouldReportEveryComplaintOfASingleBadEntry() {
      var rejections = fixture.validate(
        List.of(
          acceptableWaiver()
            .reason(" ")
            .responseCode("418")
            .parameterName("page")
            .build()
        ),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(2);
    }
  }

  @Nested
  class MisplacedWildcardTest {

    @Test
    void shouldRejectAWildcardedServiceName() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().serviceName("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).anySatisfy(rejection ->
        assertThat(rejection).contains(
          "wildcards serviceName - '*' is admitted on apiVersion only!"
        )
      );
    }

    @Test
    void shouldRejectAWildcardedApiName() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().apiName("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).anySatisfy(rejection ->
        assertThat(rejection).contains(
          "wildcards apiName - '*' is admitted on apiVersion only!"
        )
      );
    }

    @Test
    void shouldRejectAWildcardedPath() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().path("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).containsExactly(
        "Waiver { serviceName='starWars', apiName='aNewHope', apiVersion='1', path='*', method='GET' } wildcards path - '*' is admitted on apiVersion only!"
      );
    }

    @Test
    void shouldRejectAWildcardedMethod() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().method("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(
      strings = { "responseCode", "parameterName", "contentType", "criterion" }
    )
    void shouldRejectAWildcardedDiscriminator(String field) {
      var waiver = switch (field) {
        case "responseCode" -> acceptableWaiver().responseCode("*").build();
        case "parameterName" -> acceptableWaiver().parameterName("*").build();
        case "contentType" -> acceptableWaiver().contentType("*").build();
        default -> acceptableWaiver().criterion("*").build();
      };

      var rejections = fixture.validate(List.of(waiver), Set.of(A_NEW_HOPE_V1));

      assertThat(rejections).containsExactly(
        "Waiver { serviceName='starWars', apiName='aNewHope', apiVersion='1', path='/x-wings', method='GET' } wildcards %s - '*' is admitted on apiVersion only!".formatted(
          field
        )
      );
    }

    @Test
    void shouldRejectAWildcardedReason() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().reason("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).hasSize(1);
    }

    @Test
    void shouldNameEveryWildcardedFieldInOneComplaint() {
      var rejections = fixture.validate(
        List.of(acceptableWaiver().serviceName("*").apiName("*").build()),
        Set.of(A_NEW_HOPE_V1)
      );

      assertThat(rejections).anySatisfy(rejection ->
        assertThat(rejection).contains("wildcards serviceName, apiName -")
      );
    }
  }
}
