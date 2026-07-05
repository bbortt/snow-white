/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.parseOpenTelemetryData;
import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@NullMarked
@RequiredArgsConstructor
public class InfluxDBTelemetryServiceImpl implements OpenTelemetryService {

  private final InfluxDBClient influxDBClient;
  private final InfluxDBProperties influxDBProperties;

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  @Override
  @WithSpan
  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters
  ) {
    var fluxQuery = buildFluxQuery(
      apiInformation,
      lookbackFromTimestamp,
      lookbackWindow,
      attributeFilters
    );
    logger.trace("Firing flux query: {}", fluxQuery);

    var fluxTables = influxDBClient.getQueryApi().query(fluxQuery);
    return parseFluxTableToOpenTelemetryData(fluxTables);
  }

  private String buildFluxQuery(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters
  ) {
    var filteringProperties = openApiCoverageStreamProperties.getFiltering();

    var fluxBuilder = new StringBuilder();
    fluxBuilder
      .append(buildBucketFilter())
      .append(buildRangeFilter(lookbackFromTimestamp, lookbackWindow))
      .append(" |> filter(fn: (r) => r._measurement == \"spans\") ")
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getServiceNameAttributeKey(),
          apiInformation.getServiceName()
        )
      )
      .append(" |> filter(fn: (r) => r._field == \"attributes\") ")
      .append(jsonToDimensionsMapping(filteringProperties, attributeFilters))
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getApiNameAttributeKey().replace(".", "_"),
          apiInformation.getApiName()
        )
      )
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getApiVersionAttributeKey().replace(".", "_"),
          apiInformation.getApiVersion()
        )
      );

    if (!isEmpty(attributeFilters)) {
      attributeFilters.forEach(attributeFilter ->
        fluxBuilder.append(
          new FluxAttributeFilter(attributeFilter).toFluxString()
        )
      );
    }

    fluxBuilder.append(
      " |> keep(columns: [\"_value\", \"span_id\", \"trace_id\"]) "
    );

    return """
    import "date"
    import "experimental/json"

    %s
    """.formatted(fluxBuilder.toString());
  }

  private String buildBucketFilter() {
    return "from(bucket: \"" + influxDBProperties.getBucket() + "\") ";
  }

  private String buildRangeFilter(long eventTime, String lookbackWindow) {
    Instant instant = ofEpochMilli(eventTime);
    String isoTime = ISO_INSTANT.format(instant);

    return (
      "|> range(start: date.sub(d: " +
      lookbackWindow +
      ", from: " +
      isoTime +
      "), stop: " +
      isoTime +
      ")"
    );
  }

  private String jsonToDimensionsMapping(
    OpenApiCoverageStreamProperties.FilteringProperties filteringProperties,
    Set<AttributeFilter> attributeFilters
  ) {
    var apiNameMapping = filteringProperties.getApiNameAttributeKey();
    var apiVersionMapping = filteringProperties.getApiVersionAttributeKey();

    var attributesToMapToDimensions = Stream.of(
      apiNameMapping,
      apiVersionMapping
    );
    if (!isEmpty(attributeFilters)) {
      attributesToMapToDimensions = Stream.concat(
        attributesToMapToDimensions,
        attributeFilters.stream().map(AttributeFilter::key)
      );
    }

    var dimensions = attributesToMapToDimensions
      .map(
        attributeMapping ->
          attributeMapping.replace(".", "_") +
          ": parsed[\"" +
          attributeMapping +
          "\"]"
      )
      .collect(toSet());

    return """
    |> map(fn: (r) => {
      parsed = json.parse(data: bytes(v: r._value))
      return { r with %s }
    })
    """.formatted(join(", ", dimensions));
  }

  private String buildNullableAttributeFilter(
    String key,
    @Nullable String value
  ) {
    if (!hasText(value)) {
      return "";
    }

    return " |> filter(fn: (r) => r[\"" + key + "\"] == \"" + value + "\") ";
  }

  private static Set<OpenTelemetryData> parseFluxTableToOpenTelemetryData(
    List<FluxTable> fluxTables
  ) {
    Set<OpenTelemetryData> openTelemetryData = newKeySet();
    fluxTables.parallelStream().forEach(fluxTable ->
      fluxTable
        .getRecords()
        .parallelStream()
        .forEach(fluxRecord ->
          openTelemetryData.add(parseOpenTelemetryData(fluxRecord))
        )
    );
    return openTelemetryData;
  }
}
