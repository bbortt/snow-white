/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiTestResultMapperTest {

  private ApiTestResultMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiTestResultMapperImpl();
  }

  @Nested
  class FromDtos {

    @Mock
    private ApiTest apiTestMock;

    @Test
    void shouldLinkApiTestToEachResult() {
      Set<ApiTestResult> apiTestResults = fixture.fromDtos(
        Set.of(
          new OpenApiTestResult(PATH_COVERAGE, ONE, Duration.ofSeconds(1))
        ),
        apiTestMock
      );

      assertThat(apiTestResults)
        .isNotNull()
        .allSatisfy(apiTestResult ->
          assertThat(apiTestResult.getApiTest()).isEqualTo(apiTestMock)
        );
    }
  }
}
