/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

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
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageServiceProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenTelemetryService {

  private final InfluxDBClient influxDBClient;
  private final InfluxDBProperties influxDBProperties;

  private final OpenApiCoverageServiceProperties openApiCoverageServiceProperties;

  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    @Nullable Set<FluxAttributeFilter> fluxAttributeFilters
  ) {
    var fluxQuery = buildFluxQuery(
      apiInformation,
      lookbackFromTimestamp,
      lookbackWindow,
      fluxAttributeFilters
    );
    logger.trace("Firing flux query: {}", fluxQuery);

    var fluxTables = influxDBClient.getQueryApi().query(fluxQuery);
    return parseFluxTableToOpenTelemetryData(fluxTables);
  }

  private @NonNull String buildFluxQuery(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    @Nullable Set<FluxAttributeFilter> fluxAttributeFilters
  ) {
    var filteringProperties = openApiCoverageServiceProperties.getFiltering();

    var fluxBuilder = new StringBuilder();
    fluxBuilder
      .append(buildBucketFilter())
      .append(buildRangeFilter(lookbackFromTimestamp, lookbackWindow))
      .append(" |> filter(fn: (r) => r._measurement == \"spans\") ")
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getServiceNameProperty(),
          apiInformation.getServiceName()
        )
      )
      .append(" |> filter(fn: (r) => r._field == \"attributes\") ")
      .append(
        jsonToDimensionsMapping(filteringProperties, fluxAttributeFilters)
      )
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getApiNameProperty().replace(".", "_"),
          apiInformation.getApiName()
        )
      )
      .append(
        buildNullableAttributeFilter(
          filteringProperties.getApiVersionProperty().replace(".", "_"),
          apiInformation.getApiVersion()
        )
      );

    if (!isEmpty(fluxAttributeFilters)) {
      fluxAttributeFilters.forEach(attributeFilter ->
        fluxBuilder.append(attributeFilter.toFluxString())
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
    OpenApiCoverageServiceProperties.Filtering filteringProperties,
    @Nullable Set<FluxAttributeFilter> fluxAttributeFilters
  ) {
    var apiNameMapping = filteringProperties.getApiNameProperty();
    var apiVersionMapping = filteringProperties.getApiVersionProperty();

    var attributesToMapToDimensions = Stream.of(
      apiNameMapping,
      apiVersionMapping
    );
    if (!isEmpty(fluxAttributeFilters)) {
      attributesToMapToDimensions = Stream.concat(
        attributesToMapToDimensions,
        fluxAttributeFilters.stream().map(FluxAttributeFilter::getKey)
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
    @NonNull String key,
    @Nullable String value
  ) {
    if (!hasText(value)) {
      return "";
    }

    return (" |> filter(fn: (r) => r[\"" + key + "\"] == \"" + value + "\") ");
  }

  private static @NonNull Set<
    OpenTelemetryData
  > parseFluxTableToOpenTelemetryData(List<FluxTable> fluxTables) {
    Set<OpenTelemetryData> openTelemetryData = newKeySet();
    fluxTables
      .parallelStream()
      .forEach(fluxTable ->
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
