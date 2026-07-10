/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition.TempoConfiguredCondition;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.tempo.TempoAttributeFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tempo's TraceQL search API only returns attributes that are explicitly enumerated via a {@code select()} clause - it has no wildcard to return "all attributes".
 * Since downstream coverage calculators read attribute keys that can't be enumerated up front (arbitrary OpenAPI parameter/header names),
 * a search only identifies matching (traceId, spanId) pairs;
 * the full attribute set for each match is then fetched via the by-ID trace endpoint,
 * which returns the complete native span - mirroring the full attribute blob InfluxDB stores per span.
 */
@Slf4j
@Service
@NullMarked
@Conditional(TempoConfiguredCondition.class)
public class TempoTelemetryServiceImpl implements OpenTelemetryService {

  private static final String SEARCH_PATH = "/api/search";
  private static final String TRACE_BY_ID_PATH = "/api/traces/{traceId}";
  private static final int SEARCH_LIMIT = 1_000;

  private static final Pattern LOOKBACK_WINDOW_PATTERN = Pattern.compile(
    "^(\\d+)(ms|s|m|h|d|w)$"
  );

  public static final String SPAN_ATTRIBUTE = "span";
  public static final String TRACES_PROPERTY_NAME = "traces";
  public static final String SPANS_PROPERTY_NAME = "spans";
  public static final String TRACE_ID_PROPERTY_NAME = "traceID";
  public static final String SPAN_ID_PROPERTY_NAME = "spanID";

  private final RestClient tempoRestClient;
  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  public TempoTelemetryServiceImpl(
    @Qualifier("tempoRestClient") RestClient tempoRestClient,
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    this.tempoRestClient = tempoRestClient;
    this.openApiCoverageStreamProperties = openApiCoverageStreamProperties;
  }

  @Override
  @WithSpan
  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters
  ) {
    var traceQLQuery = buildTraceQLQuery(apiInformation, attributeFilters);
    logger.trace("Firing TraceQL query: {}", traceQLQuery);

    var eventInstant = ofEpochMilli(lookbackFromTimestamp);
    var startEpochSeconds = eventInstant
      .minus(parseLookbackWindow(lookbackWindow))
      .getEpochSecond();
    var endEpochSeconds = eventInstant.getEpochSecond();

    // The TraceQL query contains literal '{' / '}' characters, which UriBuilder#queryParam would otherwise misinterpret as URI template placeholders during expansion.
    // Passing it as a template variable instead keeps it an opaque, correctly-encoded value.
    var searchResponse = tempoRestClient
      .get()
      .uri(
        SEARCH_PATH + "?q={q}&start={start}&end={end}&limit={limit}",
        traceQLQuery,
        startEpochSeconds,
        endEpochSeconds,
        SEARCH_LIMIT
      )
      .retrieve()
      .body(JsonNode.class);

    return resolveMatchedSpans(searchResponse);
  }

  private String buildTraceQLQuery(
    ApiInformation apiInformation,
    Set<AttributeFilter> attributeFilters
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

    return "{ " + join(" && ", conditions) + " }";
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

    var amount = Long.parseLong(matcher.group(1));
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

  private Set<OpenTelemetryData> resolveMatchedSpans(
    @Nullable JsonNode searchResponse
  ) {
    Set<OpenTelemetryData> result = newKeySet();
    if (searchResponse == null || !searchResponse.has(TRACES_PROPERTY_NAME)) {
      return result;
    }

    searchResponse.get(TRACES_PROPERTY_NAME).forEach(trace -> {
      var traceId = trace.get(TRACE_ID_PROPERTY_NAME).asString();
      var spanSet = trace.get("spanSet");
      if (spanSet == null || !spanSet.has(SPANS_PROPERTY_NAME)) {
        return;
      }

      Set<String> matchedSpanIds = newKeySet();
      spanSet
        .get(SPANS_PROPERTY_NAME)
        .forEach(span ->
          matchedSpanIds.add(span.get(SPAN_ID_PROPERTY_NAME).asString())
        );

      result.addAll(fetchFullSpans(traceId, matchedSpanIds));
    });

    return result;
  }

  private Set<OpenTelemetryData> fetchFullSpans(
    String traceId,
    Set<String> matchedSpanIds
  ) {
    var traceResponse = tempoRestClient
      .get()
      .uri(TRACE_BY_ID_PATH, traceId)
      .retrieve()
      .body(JsonNode.class);

    Set<OpenTelemetryData> result = newKeySet();
    if (traceResponse == null || !traceResponse.has("batches")) {
      return result;
    }

    traceResponse.get("batches").forEach(batch -> {
      var scopeSpans = batch.get("scopeSpans");
      if (scopeSpans == null) {
        return;
      }

      scopeSpans.forEach(scopeSpan -> {
        var spans = scopeSpan.get(SPANS_PROPERTY_NAME);
        if (spans == null) {
          return;
        }

        spans.forEach(span -> {
          var spanId = decodeBase64SpanIdToHex(span.get("spanId").asString());
          if (matchedSpanIds.contains(spanId)) {
            result.add(
              new OpenTelemetryData(
                spanId,
                traceId,
                buildAttributes(span.get("attributes"))
              )
            );
          }
        });
      });
    });

    return result;
  }

  private static String decodeBase64SpanIdToHex(String base64SpanId) {
    return HexFormat.of().formatHex(Base64.getDecoder().decode(base64SpanId));
  }

  private static JsonNode buildAttributes(@Nullable JsonNode attributes) {
    var attributesNode = JsonMapper.shared().createObjectNode();

    if (nonNull(attributes)) {
      attributes.forEach(attribute -> {
        var value = attribute.get("value");
        if (nonNull(value) && value.has("stringValue")) {
          attributesNode.set(
            attribute.get("key").asString(),
            value.get("stringValue")
          );
        }
      });
    }

    return attributesNode;
  }
}
