/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.SPAN_ID_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.TRACE_ID_KEY;
import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.InfluxDBQueryClient;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(
  prefix = "influxdb",
  name = { "url", "token", "org", "bucket" }
)
@RealizesArch(
  ArchTraceables.ARCH_001_PLUGGABLE_INFLUXDB_OR_TEMPO_TELEMETRY_BACKEND
)
public class InfluxDBTelemetryServiceImpl implements OpenTelemetryService {

  /**
   * Human-readable name of this backend, used in {@link TelemetryBackendUnavailableException}
   * messages shown to end users.
   */
  private static final String BACKEND_NAME = "InfluxDB";

  /**
   * Prefix of the numbered per-attribute columns the Flux query projects; the suffix is the
   * attribute's index in this query's own ordered required-key list.
   */
  private static final String ATTRIBUTE_COLUMN_PREFIX = "attr_";

  private final InfluxDBQueryClient influxDBQueryClient;
  private final InfluxDBProperties influxDBProperties;

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  @Override
  @WithSpan
  @RealizesSw(
    SwTraceables.SW_023_INFLUXDB_QUERY_NARROWS_ATTRIBUTES_TO_REQUIRED_KEYS
  )
  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters,
    Set<String> requiredAttributeKeys
  ) throws TelemetryBackendUnavailableException {
    // Fixed once per query: the projected columns are numbered, so the query and the parsing of
    // its result must agree on which key each index stands for.
    var orderedAttributeKeys = List.copyOf(requiredAttributeKeys);

    var fluxQuery = buildFluxQuery(
      apiInformation,
      lookbackFromTimestamp,
      lookbackWindow,
      attributeFilters,
      orderedAttributeKeys
    );
    logger.trace("Firing flux query: {}", fluxQuery);

    List<FluxTable> fluxTables;
    try {
      fluxTables = influxDBQueryClient.query(fluxQuery);
    } catch (InfluxException e) {
      if (e.status() == 0 || e.status() >= 500) {
        throw new TelemetryBackendUnavailableException(BACKEND_NAME, e);
      }

      throw e;
    }

    return parseFluxTableToOpenTelemetryData(fluxTables, orderedAttributeKeys);
  }

  private String buildFluxQuery(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters,
    List<String> requiredAttributeKeys
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
      .append(
        jsonToDimensionsMapping(
          filteringProperties,
          attributeFilters,
          requiredAttributeKeys
        )
      )
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

    fluxBuilder.append(buildKeepClause(requiredAttributeKeys));

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
    Set<AttributeFilter> attributeFilters,
    List<String> requiredAttributeKeys
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

    var projections = attributesToMapToDimensions
      .map(
        attributeMapping ->
          attributeMapping.replace(".", "_") +
          ": parsed[\"" +
          attributeMapping +
          "\"]"
      )
      .collect(toCollection(LinkedHashSet::new));

    projections.addAll(requiredAttributeProjections(requiredAttributeKeys));

    return """
    |> map(fn: (r) => {
      parsed = json.parse(data: bytes(v: r._value))
      return { r with %s }
    })
    """.formatted(join(", ", projections));
  }

  /**
   * Each required key becomes its own column, rather than a narrowed JSON blob re-encoded in
   * Flux: attribute keys contain dots and hyphens that are not valid in a column name, so the
   * columns are numbered and mapped back to their keys by position in
   * {@link #parseFluxTableToOpenTelemetryData}.
   * <p>
   * A key absent from a given span yields no column value rather than failing the query - a span
   * carrying no {@code url.query} is ordinary, not an error.
   */
  private static List<String> requiredAttributeProjections(
    List<String> requiredAttributeKeys
  ) {
    return IntStream.range(0, requiredAttributeKeys.size())
      .mapToObj(
        index ->
          attributeColumnName(index) +
          ": (if exists parsed[\"" +
          requiredAttributeKeys.get(index) +
          "\"] then string(v: parsed[\"" +
          requiredAttributeKeys.get(index) +
          "\"]) else \"\")"
      )
      .toList();
  }

  private String buildKeepClause(List<String> requiredAttributeKeys) {
    var columns = Stream.concat(
      Stream.of(SPAN_ID_KEY, TRACE_ID_KEY),
      IntStream.range(0, requiredAttributeKeys.size()).mapToObj(
        InfluxDBTelemetryServiceImpl::attributeColumnName
      )
    )
      .map(column -> "\"" + column + "\"")
      .collect(joining(", "));

    return " |> keep(columns: [" + columns + "]) ";
  }

  private static String attributeColumnName(int index) {
    return ATTRIBUTE_COLUMN_PREFIX + index;
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
    List<FluxTable> fluxTables,
    List<String> requiredAttributeKeys
  ) {
    Set<OpenTelemetryData> openTelemetryData = newKeySet();
    fluxTables.parallelStream().forEach(fluxTable ->
      fluxTable
        .getRecords()
        .parallelStream()
        .forEach(fluxRecord ->
          openTelemetryData.add(
            parseOpenTelemetryData(fluxRecord, requiredAttributeKeys)
          )
        )
    );
    return openTelemetryData;
  }

  /**
   * Rebuilds a span's attributes from the numbered columns
   * {@link #requiredAttributeProjections} projected, mapping each back to the key at the same
   * index.
   * <p>
   * An empty column means the span did not carry that attribute, which is not the same as
   * carrying it empty - it is left off the resulting object, so a calculator's
   * {@code attributes().has(key)} check reads false, exactly as it did when the attribute was
   * absent from the full blob.
   */
  private static OpenTelemetryData parseOpenTelemetryData(
    FluxRecord fluxRecord,
    List<String> requiredAttributeKeys
  ) {
    var attributes = JsonMapper.shared().createObjectNode();

    for (var index = 0; index < requiredAttributeKeys.size(); index++) {
      var value = fluxRecord.getValueByKey(attributeColumnName(index));
      if (isNull(value) || value.toString().isEmpty()) {
        continue;
      }

      attributes.put(requiredAttributeKeys.get(index), value.toString());
    }

    return new OpenTelemetryData(
      requireNonNull(fluxRecord.getValueByKey(SPAN_ID_KEY)).toString(),
      requireNonNull(fluxRecord.getValueByKey(TRACE_ID_KEY)).toString(),
      attributes
    );
  }
}
