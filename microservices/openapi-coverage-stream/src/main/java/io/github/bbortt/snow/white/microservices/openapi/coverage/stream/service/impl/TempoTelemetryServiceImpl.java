/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.lang.Long.parseLong;
import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition.TempoConfiguredCondition;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.TempoQueryClient;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.tempo.TempoAttributeFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tempo's TraceQL search API only returns attributes that are explicitly enumerated via a {@code select()} clause - it has no wildcard to return "all attributes".
 * The keys a calculation reads are enumerable before the query fires, so they are named in a {@code select()} clause
 * and every result is built from the search response alone - no follow-up fetch per matched trace.
 * <p>
 * What the search response omits is therefore lost outright, which is why {@code spss} is always sent explicitly;
 * Tempo's own default returns three spans per span-set and reports no truncation.
 */
@Slf4j
@Service
@NullMarked
@Conditional(TempoConfiguredCondition.class)
@RealizesArch(
  ArchTraceables.ARCH_001_PLUGGABLE_INFLUXDB_OR_TEMPO_TELEMETRY_BACKEND
)
public class TempoTelemetryServiceImpl implements OpenTelemetryService {

  /**
   * Human-readable name of this backend, used in {@link TelemetryBackendUnavailableException}
   * messages shown to end users.
   */
  private static final String BACKEND_NAME = "Grafana Tempo";

  private static final int TRACE_ID_HEX_LENGTH = 32;

  private static final Pattern LOOKBACK_WINDOW_PATTERN = Pattern.compile(
    "^(\\d+)(ms|s|m|h|d|w)$"
  );

  public static final String SPAN_ATTRIBUTE = "span";
  public static final String TRACES_PROPERTY_NAME = "traces";
  public static final String SPANS_PROPERTY_NAME = "spans";
  public static final String SPAN_SETS_PROPERTY_NAME = "spanSets";
  public static final String SPAN_SET_PROPERTY_NAME = "spanSet";
  public static final String TRACE_ID_PROPERTY_NAME = "traceID";
  public static final String SPAN_ID_PROPERTY_NAME = "spanID";

  private final TempoQueryClient tempoRestClient;
  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  public TempoTelemetryServiceImpl(
    TempoQueryClient tempoRestClient,
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    this.tempoRestClient = tempoRestClient;
    this.openApiCoverageStreamProperties = openApiCoverageStreamProperties;
  }

  @Override
  @WithSpan
  @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters,
    Set<String> requiredAttributeKeys
  ) throws TelemetryBackendUnavailableException {
    var traceQLQuery = buildTraceQLQuery(
      apiInformation,
      attributeFilters,
      requiredAttributeKeys
    );
    logger.trace("Firing TraceQL query: {}", traceQLQuery);

    var eventInstant = ofEpochMilli(lookbackFromTimestamp);
    var startEpochSeconds = eventInstant
      .minus(parseLookbackWindow(lookbackWindow))
      .getEpochSecond();
    var endEpochSeconds = eventInstant.getEpochSecond();

    JsonNode searchResponse;
    try {
      searchResponse = tempoRestClient.search(
        traceQLQuery,
        startEpochSeconds,
        endEpochSeconds
      );
    } catch (HttpServerErrorException | ResourceAccessException e) {
      throw new TelemetryBackendUnavailableException(BACKEND_NAME, e);
    }

    return resolveMatchedSpans(searchResponse);
  }

  private String buildTraceQLQuery(
    ApiInformation apiInformation,
    Set<AttributeFilter> attributeFilters,
    Set<String> requiredAttributeKeys
  ) {
    var filteringProperties = openApiCoverageStreamProperties.getFiltering();

    var conditions = Stream.of(
      buildNullableAttributeCondition(
        "resource",
        filteringProperties.getServiceNameAttributeKey(),
        apiInformation.getServiceName()
      ),
      buildNullableAttributeCondition(
        SPAN_ATTRIBUTE,
        filteringProperties.getApiNameAttributeKey(),
        apiInformation.getApiName()
      ),
      buildNullableAttributeCondition(
        SPAN_ATTRIBUTE,
        filteringProperties.getApiVersionAttributeKey(),
        apiInformation.getApiVersion()
      )
    )
      .filter(Objects::nonNull)
      .collect(toCollection(ArrayList::new));

    if (!isEmpty(attributeFilters)) {
      attributeFilters.forEach(attributeFilter ->
        conditions.add(
          new TempoAttributeFilter(attributeFilter).toTraceQLString()
        )
      );
    }

    return (
      "{ " +
      join(" && ", conditions) +
      " }" +
      buildSelectClause(requiredAttributeKeys)
    );
  }

  /**
   * Attribute names are always quoted.
   * Every required key is dotted, and an unquoted dotted name is ambiguous to TraceQL's parser
   * (a span attribute literally named {@code resource.x} being the pathological case);
   * header keys additionally carry hyphens, which would otherwise terminate the name.
   *
   * @see <a href="https://grafana.com/docs/tempo/latest/traceql/construct-traceql-queries/">Construct a TraceQL query</a>
   */
  private static String buildSelectClause(Set<String> requiredAttributeKeys) {
    if (isEmpty(requiredAttributeKeys)) {
      return "";
    }

    return requiredAttributeKeys
      .stream()
      .map(key -> SPAN_ATTRIBUTE + ".\"" + escapeAttributeName(key) + "\"")
      .collect(joining(", ", " | select(", ")"));
  }

  /**
   * TraceQL supports exactly two escape sequences inside a quoted attribute name.
   */
  private static String escapeAttributeName(String attributeName) {
    return attributeName.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static @Nullable String buildNullableAttributeCondition(
    String scope,
    String key,
    @Nullable String value
  ) {
    if (!hasText(value)) {
      return null;
    }

    return scope + "." + key + " = \"" + value + "\"";
  }

  private static Duration parseLookbackWindow(String lookbackWindow) {
    var matcher = LOOKBACK_WINDOW_PATTERN.matcher(lookbackWindow);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        "Unparseable lookback window: " + lookbackWindow
      );
    }

    var amount = parseLong(matcher.group(1));
    return switch (matcher.group(2)) {
      case "ms" -> Duration.ofMillis(amount);
      case "s" -> Duration.ofSeconds(amount);
      case "m" -> Duration.ofMinutes(amount);
      case "h" -> Duration.ofHours(amount);
      case "d" -> Duration.ofDays(amount);
      case "w" -> Duration.ofDays(amount * 7);
      default -> throw new IllegalArgumentException(
        "Unsupported lookback window unit: " + matcher.group(2)
      );
    };
  }

  @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
  private static Set<OpenTelemetryData> resolveMatchedSpans(
    @Nullable JsonNode searchResponse
  ) {
    Set<OpenTelemetryData> result = newKeySet();
    if (isNull(searchResponse) || !searchResponse.has(TRACES_PROPERTY_NAME)) {
      return result;
    }

    for (var trace : searchResponse.get(TRACES_PROPERTY_NAME)) {
      var traceId = normalizeTraceId(
        trace.get(TRACE_ID_PROPERTY_NAME).asString()
      );

      for (var spanSet : resolveSpanSets(trace)) {
        if (!spanSet.has(SPANS_PROPERTY_NAME)) {
          continue;
        }

        spanSet
          .get(SPANS_PROPERTY_NAME)
          .forEach(span ->
            result.add(
              new OpenTelemetryData(
                span.get(SPAN_ID_PROPERTY_NAME).asString(),
                traceId,
                buildAttributes(span.get("attributes"))
              )
            )
          );
      }
    }

    return result;
  }

  /**
   * Tempo populates both {@code spanSets} and the deprecated singular {@code spanSet} with the
   * same spans, so reading both would count every span twice.
   * Only {@code spanSets} can carry more
   * than one span set, which grouping queries produce - this backend issues none today, but
   * reading the array keeps that a query-shape change rather than a silent data loss.
   */
  private static List<JsonNode> resolveSpanSets(JsonNode trace) {
    var spanSets = trace.get(SPAN_SETS_PROPERTY_NAME);
    if (nonNull(spanSets) && !spanSets.isEmpty()) {
      return stream(spanSets.spliterator(), false).toList();
    }

    var spanSet = trace.get(SPAN_SET_PROPERTY_NAME);
    return isNull(spanSet) ? List.of() : List.of(spanSet);
  }

  /**
   * Tempo's search API renders trace IDs using Jaeger-compatible hex formatting,
   * which strips leading zero nibbles instead of zero-padding to the full 128-bit (32 hex character) trace ID length.
   */
  private static String normalizeTraceId(String traceId) {
    return traceId.length() < TRACE_ID_HEX_LENGTH
      ? "0".repeat(TRACE_ID_HEX_LENGTH - traceId.length()) + traceId
      : traceId;
  }

  /**
   * An OTLP/JSON attribute's {@code value} is a typed union (protobuf {@code oneof}) - real
   * instrumentation encodes e.g. {@code http.response.status_code} (a {@code Long}-typed semconv
   * attribute) as {@code intValue}, not {@code stringValue}. Every downstream coverage calculator
   * only ever reads attributes back out via {@code JsonNode.asString()}, so each wrapper is
   * unwrapped to its scalar node here rather than kept as a distinct Java type.
   */
  private static final List<String> ATTRIBUTE_VALUE_KEYS = List.of(
    "stringValue",
    "intValue",
    "doubleValue",
    "boolValue"
  );

  private static JsonNode buildAttributes(@Nullable JsonNode attributes) {
    var attributesNode = JsonMapper.shared().createObjectNode();

    if (nonNull(attributes)) {
      attributes.forEach(attribute -> {
        var value = attribute.get("value");
        if (isNull(value)) {
          return;
        }

        ATTRIBUTE_VALUE_KEYS.stream()
          .filter(value::has)
          .findFirst()
          .ifPresent(valueKey ->
            attributesNode.set(
              attribute.get("key").asString(),
              value.get(valueKey)
            )
          );
      });
    }

    return attributesNode;
  }
}
