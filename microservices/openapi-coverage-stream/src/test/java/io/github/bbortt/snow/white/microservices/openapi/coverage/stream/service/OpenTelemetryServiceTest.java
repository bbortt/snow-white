/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.SPAN_ID_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.TRACE_ID_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.VALUE_KEY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class OpenTelemetryServiceTest {

  @Mock
  private InfluxDBClient influxDBClientMock;

  private InfluxDBProperties influxDBProperties;

  private OpenTelemetryService fixture;

  @BeforeEach
  void beforeEachSetup() {
    influxDBProperties = new InfluxDBProperties();

    var openApiCoverageServiceProperties =
      new OpenApiCoverageStreamProperties();

    fixture = new OpenTelemetryService(
      influxDBClientMock,
      influxDBProperties,
      openApiCoverageServiceProperties
    );
  }

  @Nested
  class FindOpenTelemetryTracingData {

    private static final ApiInformation API_INFORMATION =
      ApiInformation.builder()
        .serviceName("serviceName")
        .apiName("apiName")
        .build();

    private static final long LOOKBACK_FROM = 1234L;
    private static final String LOOKBACK_WINDOW = "1h";
    private static final String OTEL_BUCKET = "otel-bucket";

    @Mock
    private QueryApi queryApi;

    @Mock
    private FluxTable fluxTable;

    @Mock
    private FluxRecord fluxRecord;

    @BeforeEach
    void beforeEachSetup() {
      influxDBProperties.setBucket(OTEL_BUCKET);
      doReturn(queryApi).when(influxDBClientMock).getQueryApi();
    }

    public static Stream<
      Set<FluxAttributeFilter>
    > withoutAttributeFilters_shouldBuildCorrectQuery() {
      return Stream.of(null, emptySet());
    }

    @MethodSource
    @ParameterizedTest
    void withoutAttributeFilters_shouldBuildCorrectQuery(
      Set<FluxAttributeFilter> fluxAttributeFilters
    ) {
      ArgumentCaptor<String> queryCaptor = captor();
      doReturn(emptyList()).when(queryApi).query(queryCaptor.capture());

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        fluxAttributeFilters
      );

      assertThat(result).isEmpty();

      var capturedQuery = queryCaptor.getValue();
      assertThat(capturedQuery).contains(
        "import \"date\"",
        "import \"experimental/json\"",
        "from(bucket: \"" + OTEL_BUCKET + "\")",
        "|> range(start: date.sub(d: " +
          LOOKBACK_WINDOW +
          ", from: 1970-01-01T00:00:01.234Z), stop: 1970-01-01T00:00:01.234Z)",
        "|> filter(fn: (r) => r._measurement == \"spans\")",
        "|> filter(fn: (r) => r[\"service.name\"] == \"" +
          API_INFORMATION.getServiceName() +
          "\")",
        "|> filter(fn: (r) => r._field == \"attributes\")",
        """
        |> map(fn: (r) => {
          parsed = json.parse(data: bytes(v: r._value))
          return { r with api_name: parsed["api.name"], api_version: parsed["api.version"] }
        })
        """,
        "|> filter(fn: (r) => r[\"api_name\"] == \"" +
          API_INFORMATION.getApiName() +
          "\")",
        "|> keep(columns: [\"_value\", \"span_id\", \"trace_id\"])"
      );
    }

    @Test
    void withApiVersion_shouldAppendFilterExpression() {
      ArgumentCaptor<String> queryCaptor = captor();
      doReturn(emptyList()).when(queryApi).query(queryCaptor.capture());

      var apiInformation = API_INFORMATION.withApiVersion("apiVersion");

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        apiInformation,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet()
      );

      assertThat(result).isEmpty();

      var capturedQuery = queryCaptor.getValue();
      assertThat(capturedQuery).contains(
        "import \"date\"",
        "import \"experimental/json\"",
        "from(bucket: \"" + OTEL_BUCKET + "\")",
        "|> range(start: date.sub(d: " +
          LOOKBACK_WINDOW +
          ", from: 1970-01-01T00:00:01.234Z), stop: 1970-01-01T00:00:01.234Z)",
        "|> filter(fn: (r) => r._measurement == \"spans\")",
        "|> filter(fn: (r) => r[\"service.name\"] == \"" +
          apiInformation.getServiceName() +
          "\")",
        "|> filter(fn: (r) => r._field == \"attributes\")",
        """
        |> map(fn: (r) => {
          parsed = json.parse(data: bytes(v: r._value))
          return { r with api_name: parsed["api.name"], api_version: parsed["api.version"] }
        })
        """,
        "|> filter(fn: (r) => r[\"api_name\"] == \"" +
          apiInformation.getApiName() +
          "\")",
        "|> filter(fn: (r) => r[\"api_version\"] == \"" +
          apiInformation.getApiVersion() +
          "\")",
        "|> keep(columns: [\"_value\", \"span_id\", \"trace_id\"]) "
      );
    }

    @Test
    void withAttributeFilters_shouldIncludeFiltersInQuery() {
      var filter1 = new FluxAttributeFilter(
        new AttributeFilter("http.method", STRING_EQUALS, "GET")
      );
      var filter2 = new FluxAttributeFilter(
        new AttributeFilter("http.status_code", STRING_EQUALS, "200")
      );
      Set<FluxAttributeFilter> fluxAttributeFilters = Set.of(filter1, filter2);

      ArgumentCaptor<String> queryCaptor = captor();
      doReturn(emptyList()).when(queryApi).query(queryCaptor.capture());

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        fluxAttributeFilters
      );

      assertThat(result).isEmpty();

      String capturedQuery = queryCaptor.getValue();
      assertThat(capturedQuery).contains(
        "import \"date\"",
        "import \"experimental/json\"",
        "from(bucket: \"" + OTEL_BUCKET + "\")",
        "|> range(start: date.sub(d: " +
          LOOKBACK_WINDOW +
          ", from: 1970-01-01T00:00:01.234Z), stop: 1970-01-01T00:00:01.234Z)",
        "|> filter(fn: (r) => r._measurement == \"spans\")",
        "|> filter(fn: (r) => r[\"service.name\"] == \"" +
          API_INFORMATION.getServiceName() +
          "\")",
        "|> filter(fn: (r) => r._field == \"attributes\")",
        """
        |> map(fn: (r) => {
          parsed = json.parse(data: bytes(v: r._value))
          return { r with api_name: parsed["api.name"], api_version: parsed["api.version"], http_method: parsed["http.method"], http_status_code: parsed["http.status_code"] }
        })
        """,
        "|> filter(fn: (r) => r[\"api_name\"] == \"" +
          API_INFORMATION.getApiName() +
          "\")",
        "|> filter(fn: (r) => r.http_method == \"GET\")",
        "|> filter(fn: (r) => r.http_status_code == \"200\")",
        "|> keep(columns: [\"_value\", \"span_id\", \"trace_id\"])"
      );
    }

    @Test
    void withResults_shouldParseOpenTelemetryData() {
      // Prepare first record
      var spanId1 = "3f1a2c9e7d4b8a61";
      var traceId1 = "f2c79a8d4bce407aa65c1e7289f6febb";
      var attributesValue1 =
        // language=json
        """
          {"http.method":"GET","http.path":"/api/v1/test"}
          """;

      var fluxRecord1 = mock(FluxRecord.class);
      doReturn(spanId1).when(fluxRecord1).getValueByKey(SPAN_ID_KEY);
      doReturn(traceId1).when(fluxRecord1).getValueByKey(TRACE_ID_KEY);
      doReturn(attributesValue1).when(fluxRecord1).getValueByKey(VALUE_KEY);

      // Prepare second record
      var spanId2 = "8a7d2e4b9c3f1d0a";
      var traceId2 = "b1e24f988ab04129be3e2cd9275c991a";
      var attributesValue2 =
        // language=json
        """
          {"http.method":"POST","http.path":"/api/v1/create"}
          """;

      var fluxRecord2 = mock(FluxRecord.class);
      doReturn(spanId2).when(fluxRecord2).getValueByKey(SPAN_ID_KEY);
      doReturn(traceId2).when(fluxRecord2).getValueByKey(TRACE_ID_KEY);
      doReturn(attributesValue2).when(fluxRecord2).getValueByKey(VALUE_KEY);

      doReturn(List.of(fluxRecord1, fluxRecord2)).when(fluxTable).getRecords();
      doReturn(singletonList(fluxTable)).when(queryApi).query(anyString());

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        null
      );

      assertThat(result)
        .hasSize(2)
        .satisfiesExactlyInAnyOrder(
          data1 -> {
            assertThat(data1.spanId()).isEqualTo(spanId1);
            assertThat(data1.traceId()).isEqualTo(traceId1);
            assertThat(data1.attributes()).isEqualTo(
              JsonMapper.shared().readTree(attributesValue1)
            );
          },
          data2 -> {
            assertThat(data2.spanId()).isEqualTo(spanId2);
            assertThat(data2.traceId()).isEqualTo(traceId2);
            assertThat(data2.attributes()).isEqualTo(
              JsonMapper.shared().readTree(attributesValue2)
            );
          }
        );
    }
  }
}
