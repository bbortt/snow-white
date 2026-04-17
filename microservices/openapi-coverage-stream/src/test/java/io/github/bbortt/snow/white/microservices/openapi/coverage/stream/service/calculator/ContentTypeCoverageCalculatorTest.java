/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ContentTypeCoverageCalculator.CONTENT_TYPE_HEADER_KEY;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class ContentTypeCoverageCalculatorTest {

  private ContentTypeCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ContentTypeCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenContentTypeCoverage() {
      boolean result = fixture.accepts(CONTENT_TYPE_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotContentTypeCoverage(
      OpenApiCriteria openApiCriteria
    ) {
      if (CONTENT_TYPE_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json", "multipart/form-data")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(
          telemetryWithContentType("application/json"),
          telemetryWithContentType("multipart/form-data")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn50Percent_whenSomeContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/documents",
        operationWithContentTypes("application/json", "multipart/form-data")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/documents",
        List.of(telemetryWithContentType("application/json"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following request body content types are uncovered: `POST_/api/v1/documents [multipart/form-data]`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following request body content types are uncovered: `POST_/api/v1/users [application/json]`"
          )
      );
    }

    @Test
    void shouldSkipOperationsWithNoRequestBody() {
      var operationWithoutBody = new Operation();

      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithoutBody
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // 0/0 = 100%
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldMatchContentTypeWithCharsetSuffix() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      // Observed value includes charset parameter — should still match
      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetryWithContentType("application/json; charset=utf-8"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleContentTypeAsJsonArray() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var attributes = JsonMapper.shared().createObjectNode();
      var arrayNode = attributes.putArray(CONTENT_TYPE_HEADER_KEY);
      arrayNode.add("application/json");

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(new OpenTelemetryData("span-1", "trace-1", attributes))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleMultipleOperations() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json"),
        "PUT_/api/v1/users/{id}",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetryWithContentType("application/json")),
        "PUT_/api/v1/users/42",
        List.of(telemetryWithContentType("application/json"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    private Operation operationWithContentTypes(String... contentTypes) {
      var content = new Content();
      for (String contentType : contentTypes) {
        content.addMediaType(contentType, new MediaType());
      }

      var requestBody = new RequestBody();
      requestBody.setContent(content);

      var operation = new Operation();
      operation.setRequestBody(requestBody);
      return operation;
    }

    private OpenTelemetryData telemetryWithContentType(String contentType) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put(CONTENT_TYPE_HEADER_KEY, contentType);
      return new OpenTelemetryData("span-1", "trace-1", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }
}
