/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WaiverUnitTest {

  private static final String PATH = "/pets/{petId}";
  private static final String REASON = "the downstream cannot be made to fail";
  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);

  private static Waiver waiver(
    String method,
    @Nullable String responseCode,
    @Nullable String parameterName,
    @Nullable String contentType
  ) {
    return new Waiver(
      PATH,
      method,
      responseCode,
      parameterName,
      contentType,
      null,
      REASON,
      EXPIRES
    );
  }

  private static ApiTestFinding finding(
    String path,
    @Nullable String method,
    @Nullable String responseCode,
    @Nullable String parameterName,
    @Nullable String contentType
  ) {
    return ApiTestFinding.builder()
      .specPointer("/paths/~1pets~1{petId}")
      .status(UNCOVERED)
      .httpPath(path)
      .httpMethod(method)
      .responseCode(responseCode)
      .parameterName(parameterName)
      .contentType(contentType)
      .evidence(List.of())
      .build();
  }

  @Nested
  @VerifiesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
  class AddressesTest {

    @Test
    void shouldAddressTheOperationItself() {
      var result = waiver("GET", null, null, null).addresses(
        finding(PATH, "GET", null, null, null)
      );

      assertThat(result).isTrue();
    }

    @Test
    void shouldAddressAResponseCode() {
      var result = waiver("GET", "404", null, null).addresses(
        finding(PATH, "GET", "404", null, null)
      );

      assertThat(result).isTrue();
    }

    @Test
    void shouldAddressAParameter() {
      var result = waiver("GET", null, "petId", null).addresses(
        finding(PATH, "GET", null, "petId", null)
      );

      assertThat(result).isTrue();
    }

    @Test
    void shouldAddressAContentType() {
      var result = waiver("GET", null, null, "application/json").addresses(
        finding(PATH, "GET", null, null, "application/json")
      );

      assertThat(result).isTrue();
    }

    @Test
    void shouldNotAddressAnotherPath() {
      var result = waiver("GET", null, null, null).addresses(
        finding("/pets", "GET", null, null, null)
      );

      assertThat(result).isFalse();
    }

    @Test
    void shouldNotAddressAnotherMethod() {
      var result = waiver("GET", null, null, null).addresses(
        finding(PATH, "DELETE", null, null, null)
      );

      assertThat(result).isFalse();
    }

    /**
     * An author writes the method as their specification spells it; a finding carries it as the
     * operation key does. The two disagreeing on case must not make the entry inert.
     */
    @Test
    void shouldAddressRegardlessOfHowTheMethodWasSpelled() {
      var result = waiver("get", null, null, null).addresses(
        finding(PATH, "GET", null, null, null)
      );

      assertThat(result).isTrue();
    }

    @Test
    void shouldNotAddressAFindingThatNamesNoMethod() {
      var result = waiver("GET", null, null, null).addresses(
        finding(PATH, null, null, null, null)
      );

      assertThat(result).isFalse();
    }

    /**
     * The omission is the claim: a waiver carrying no response code names the operation, not every
     * response under it. Widening it would make the narrowest entry an author can write the
     * broadest one in effect.
     */
    @Test
    void shouldNotAddressADiscriminatedFinding_whenTheWaiverOmittedTheDiscriminator() {
      var result = waiver("GET", null, null, null).addresses(
        finding(PATH, "GET", "404", null, null)
      );

      assertThat(result).isFalse();
    }

    @Test
    void shouldNotAddressTheOperation_whenTheWaiverNamedAResponseCode() {
      var result = waiver("GET", "404", null, null).addresses(
        finding(PATH, "GET", null, null, null)
      );

      assertThat(result).isFalse();
    }

    @Test
    void shouldNotAddressAnotherResponseCodeOfTheSameOperation() {
      var result = waiver("GET", "404", null, null).addresses(
        finding(PATH, "GET", "500", null, null)
      );

      assertThat(result).isFalse();
    }

    @Test
    void shouldNotAddressAnotherParameterOfTheSameOperation() {
      var result = waiver("GET", null, "petId", null).addresses(
        finding(PATH, "GET", null, "expand", null)
      );

      assertThat(result).isFalse();
    }

    @Test
    void shouldNotAddressAnotherContentTypeOfTheSameOperation() {
      var result = waiver("GET", null, null, "application/json").addresses(
        finding(PATH, "GET", null, null, "application/xml")
      );

      assertThat(result).isFalse();
    }

    /**
     * Which criterion a waiver names is a separate question from which target it addresses, and
     * answered elsewhere. Were it answered here, an entry naming a criterion the gate excluded
     * would read as addressing nothing at all.
     */
    @Test
    void shouldAddressTheTargetRegardlessOfTheCriterionItNamed() {
      var namingACriterion = new Waiver(
        PATH,
        "GET",
        "404",
        null,
        null,
        "ERROR_RESPONSE_CODE_COVERAGE",
        REASON,
        EXPIRES
      );

      var result = namingACriterion.addresses(
        finding(PATH, "GET", "404", null, null)
      );

      assertThat(result).isTrue();
    }
  }

  @Nested
  class TargetTest {

    @Test
    void shouldNameTheOperation() {
      var result = waiver("GET", null, null, null).target();

      assertThat(result).isEqualTo("GET /pets/{petId}");
    }

    @Test
    void shouldUpperCaseTheMethodItWasGiven() {
      var result = waiver("get", null, null, null).target();

      assertThat(result).isEqualTo("GET /pets/{petId}");
    }

    @Test
    void shouldNameTheResponseCode() {
      var result = waiver("GET", "404", null, null).target();

      assertThat(result).isEqualTo("GET /pets/{petId} (404)");
    }

    @Test
    void shouldNameTheParameter() {
      var result = waiver("GET", null, "petId", null).target();

      assertThat(result).isEqualTo("GET /pets/{petId} (petId)");
    }

    @Test
    void shouldNameTheContentType() {
      var result = waiver("GET", null, null, "application/json").target();

      assertThat(result).isEqualTo("GET /pets/{petId} (application/json)");
    }
  }
}
