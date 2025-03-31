package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.query.FluxRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record OpenTelemetryData(
  String spanId,
  String traceId,
  JsonNode attributes
) {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static final String SPAN_ID_KEY = "span_id";
  public static final String TRACE_ID_KEY = "trace_id";
  public static final String VALUE_KEY = "_value";

  public static OpenTelemetryData parseOpenTelemetryData(
    FluxRecord fluxRecord
  ) {
    return new OpenTelemetryData(
      requireNonNull(fluxRecord.getValueByKey(SPAN_ID_KEY)).toString(),
      requireNonNull(fluxRecord.getValueByKey(TRACE_ID_KEY)).toString(),
      parseJsonAttributes(
        requireNonNull(fluxRecord.getValueByKey(VALUE_KEY)).toString()
      )
    );
  }

  private static JsonNode parseJsonAttributes(String attributes) {
    try {
      return objectMapper.readTree(attributes);
    } catch (JsonProcessingException e) {
      logger.error("Failed parsing span attributes!", e);
      return objectMapper.createObjectNode();
    }
  }
}
