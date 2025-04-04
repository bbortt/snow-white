package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData.SPAN_ID_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData.TRACE_ID_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData.VALUE_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilterOperator.STRING_EQUALS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilter;
import java.util.List;
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

@ExtendWith({ MockitoExtension.class })
class OpenTelemetryServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private InfluxDBClient influxDBClientMock;

  private InfluxDBProperties influxDBProperties;

  private OpenTelemetryService fixture;

  @BeforeEach
  void beforeEachSetup() {
    influxDBProperties = new InfluxDBProperties();

    fixture = new OpenTelemetryService(influxDBClientMock, influxDBProperties);
  }

  @Nested
  class FindTracingData {

    private static final String SERVICE_NAME = "test-service";
    private static final String LOOKBACK_RANGE = "1h";
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
      List<AttributeFilter>
    > withoutAttributeFilters_shouldBuildCorrectQuery() {
      return Stream.of(null, emptyList());
    }

    @MethodSource
    @ParameterizedTest
    void withoutAttributeFilters_shouldBuildCorrectQuery(
      List<AttributeFilter> attributeFilters
    ) {
      doReturn(emptyList()).when(queryApi).query(anyString());

      List<OpenTelemetryData> result = fixture.findTracingData(
        SERVICE_NAME,
        LOOKBACK_RANGE,
        attributeFilters
      );

      assertThat(result).isEmpty();

      ArgumentCaptor<String> queryCaptor = captor();
      verify(queryApi).query(queryCaptor.capture());

      var capturedQuery = queryCaptor.getValue();
      assertThat(capturedQuery).contains(
        "from(bucket: \"" + OTEL_BUCKET + "\")",
        "|> range(start: -" + LOOKBACK_RANGE + ")",
        "r._measurement == \"spans\"",
        "r[\"service.name\"] == \"" + SERVICE_NAME + "\"",
        "r._field == \"attributes\"",
        "keep(columns: [\"_field\", \"_value\", \"span_id\", \"trace_id\"])"
      );
    }

    @Test
    void withAttributeFilters_shouldIncludeFiltersInQuery() {
      var filter1 = new AttributeFilter("http.method", STRING_EQUALS, "GET");
      var filter2 = new AttributeFilter(
        "http.status_code",
        STRING_EQUALS,
        "200"
      );
      List<AttributeFilter> attributeFilters = List.of(filter1, filter2);

      doReturn(emptyList()).when(queryApi).query(anyString());

      List<OpenTelemetryData> result = fixture.findTracingData(
        SERVICE_NAME,
        LOOKBACK_RANGE,
        attributeFilters
      );

      assertThat(result).isEmpty();

      ArgumentCaptor<String> queryCaptor = captor();
      verify(queryApi).query(queryCaptor.capture());

      String capturedQuery = queryCaptor.getValue();
      assertThat(capturedQuery).contains(
        "from(bucket: \"" + OTEL_BUCKET + "\")",
        "|> range(start: -" + LOOKBACK_RANGE + ")",
        "r._measurement == \"spans\"",
        "r[\"service.name\"] == \"" + SERVICE_NAME + "\"",
        "r._field == \"attributes\"",
        filter1.toFluxString(),
        filter2.toFluxString(),
        "keep(columns: [\"_field\", \"_value\", \"span_id\", \"trace_id\"])"
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

      List<OpenTelemetryData> result = fixture.findTracingData(
        SERVICE_NAME,
        LOOKBACK_RANGE,
        null
      );

      assertThat(result)
        .hasSize(2)
        .satisfiesExactly(
          data1 -> {
            assertThat(data1.spanId()).isEqualTo(spanId1);
            assertThat(data1.traceId()).isEqualTo(traceId1);
            assertThat(data1.attributes()).isEqualTo(
              OBJECT_MAPPER.readTree(attributesValue1)
            );
          },
          data2 -> {
            assertThat(data2.spanId()).isEqualTo(spanId2);
            assertThat(data2.traceId()).isEqualTo(traceId2);
            assertThat(data2.attributes()).isEqualTo(
              OBJECT_MAPPER.readTree(attributesValue2)
            );
          }
        );
    }
  }
}
