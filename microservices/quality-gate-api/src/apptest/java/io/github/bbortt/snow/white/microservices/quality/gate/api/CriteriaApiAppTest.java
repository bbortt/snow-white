/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import java.util.List;
import org.assertj.core.api.ThrowingConsumer;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.criteria.api.CriteriaApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@CitrusSupport
class CriteriaApiAppTest {

  private static CriteriaApi criteriaApi;

  @BeforeAll
  static void beforeAllSetup() {
    criteriaApi = new CriteriaApi(
      getHttpEndpoint(
        getProperty("quality-gate-api.host", "localhost"),
        parseInt(getProperty("quality-gate-api.port", "8081"))
      )
    );
  }

  /**
   * Verifies that the criteria endpoint returns the complete, correctly mapped list of built-in OpenAPI coverage criteria.
   *
   * <p>
   * When requesting {@code GET /api/rest/v1/criteria/openapi}, the service must:
   * <ul>
   *   <li>return HTTP 200</li>
   *   <li>return a JSON array whose length equals the number of {@link OpenApiCoverageCriteria} enum constants</li>
   *   <li>contain exactly one entry per enum constant, matched in any order, where each entry's
   *       {@code id}, {@code name}, and {@code description} correspond to the enum's {@code name()},
   *       {@code getLabel()}, and {@code getDescription()} respectively</li>
   * </ul>
   *
   * <p>
   * Basic response schema validation (required fields, types) is performed automatically by the Citrus OpenAPI integration.
   * The inline validator asserts the deeper contract - that the application exposes every known criterion and maps all fields correctly.
   */
  @Test
  @CitrusTest
  void shouldGetAllOpenApiCoverageCriteria(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(criteriaApi.sendListOpenApiCoverageCriteria());

    testRunner.then(
      criteriaApi
        .receiveListOpenApiCoverageCriteria(OK)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          var openApiCriteria = JsonMapper.shared().readValue(
            payload,
            new TypeReference<List<OpenApiCriterion>>() {}
          );

          assertThat(openApiCriteria)
            .hasSize(OpenApiCoverageCriteria.values().length)
            .satisfiesExactlyInAnyOrder(
              stream(OpenApiCoverageCriteria.values())
                .map(
                  openApiCoverageCriteria ->
                    (ThrowingConsumer<OpenApiCriterion>) actual -> {
                      assertThat(actual.getId()).isEqualTo(
                        openApiCoverageCriteria.name()
                      );
                      assertThat(actual.getName()).isEqualTo(
                        openApiCoverageCriteria.getLabel()
                      );
                      assertThat(actual.getDescription()).isEqualTo(
                        openApiCoverageCriteria.getDescription()
                      );
                    }
                )
                .toArray(ThrowingConsumer[]::new)
            );
        })
    );
  }
}
