/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service;

import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OtelInformationFilteringService {

  private final CachingService cachingService;

  private final String apiNameProperty;
  private final String apiVersionProperty;
  private final String serviceNameProperty;

  public OtelInformationFilteringService(
    CachingService cachingService,
    OtelEventFilterStreamProperties otelEventFilterStreamProperties
  ) {
    this.cachingService = cachingService;

    var filteringProperties = otelEventFilterStreamProperties.getFiltering();
    this.apiNameProperty = filteringProperties.getApiNameProperty();
    this.apiVersionProperty = filteringProperties.getApiVersionProperty();
    this.serviceNameProperty = filteringProperties.getServiceNameProperty();

    logger.info("Filter is in place: {}", filteringProperties);
  }

  public @NonNull ExportTraceServiceRequest filterUnknownSpecifications(
    ExportTraceServiceRequest exportTraceServiceRequest
  ) {
    return ExportTraceServiceRequest.newBuilder()
      .addAllResourceSpans(
        exportTraceServiceRequest
          .getResourceSpansList()
          .stream()
          .map(this::filterResourceSpansDropAllOfUnknownSpecifications)
          .toList()
      )
      .build();
  }

  private @NonNull ResourceSpans filterResourceSpansDropAllOfUnknownSpecifications(
    ResourceSpans resourceSpans
  ) {
    var resourceAttributes = resourceSpans.getResource().getAttributesList();
    var apiIdentifier = extractApiIdentifyingAttributes(resourceAttributes);

    if (apiIdentifier.isPresent()) {
      if (apiIsKnownToSnowWhite(apiIdentifier.get())) {
        return resourceSpans;
      }

      return ResourceSpans.newBuilder(resourceSpans).clearScopeSpans().build();
    }

    return ResourceSpans.newBuilder(resourceSpans)
      .clearScopeSpans()
      .addAllScopeSpans(
        resourceSpans
          .getScopeSpansList()
          .stream()
          .map(scopeSpans ->
            filterScopeSpansDropAllOfUnknownSpecifications(
              scopeSpans,
              resourceAttributes
            )
          )
          .toList()
      )
      .build();
  }

  private @NonNull ScopeSpans filterScopeSpansDropAllOfUnknownSpecifications(
    ScopeSpans scopeSpans,
    List<KeyValue> resourceAttributes
  ) {
    var scopeAttributes = scopeSpans.getScope().getAttributesList();
    var apiIdentifier = extractApiIdentifyingAttributes(
      concat(resourceAttributes.stream(), scopeAttributes.stream()).toList()
    );

    if (apiIdentifier.isPresent()) {
      if (apiIsKnownToSnowWhite(apiIdentifier.get())) {
        return scopeSpans;
      }

      return ScopeSpans.newBuilder(scopeSpans).clearSpans().build();
    }

    return ScopeSpans.newBuilder(scopeSpans)
      .clearSpans()
      .addAllSpans(
        scopeSpans
          .getSpansList()
          .stream()
          .map(span ->
            filterSpanReturnNullWhenSpecificationIsUnknown(
              span,
              resourceAttributes,
              scopeAttributes
            )
          )
          .filter(Objects::nonNull)
          .toList()
      )
      .build();
  }

  private @Nullable Span filterSpanReturnNullWhenSpecificationIsUnknown(
    Span span,
    List<KeyValue> resourceAttributes,
    List<KeyValue> scopeAttributes
  ) {
    var spanAttributes = span.getAttributesList();
    var apiIdentifier = extractApiIdentifyingAttributes(
      Stream.of(
        resourceAttributes.stream(),
        scopeAttributes.stream(),
        spanAttributes.stream()
      )
        .flatMap(identity())
        .toList()
    );

    if (
      apiIdentifier.isPresent() && apiIsKnownToSnowWhite(apiIdentifier.get())
    ) {
      return span;
    }

    return null;
  }

  private boolean apiIsKnownToSnowWhite(ApiIdentifier apiIdentifier) {
    return cachingService.apiExists(
      apiIdentifier.otelServiceName,
      apiIdentifier.apiName,
      apiIdentifier.apiVersion
    );
  }

  private Optional<ApiIdentifier> extractApiIdentifyingAttributes(
    List<KeyValue> attributes
  ) {
    var apiName = attributes
      .stream()
      .filter(attribute -> attribute.getKey().equals(apiNameProperty))
      .findFirst()
      .map(KeyValue::getValue)
      .map(AnyValue::getStringValue)
      .orElse(null);
    var apiVersion = attributes
      .stream()
      .filter(attribute -> attribute.getKey().equals(apiVersionProperty))
      .findFirst()
      .map(KeyValue::getValue)
      .map(AnyValue::getStringValue)
      .orElse(null);
    var otelServiceName = attributes
      .stream()
      .filter(attribute -> attribute.getKey().equals(serviceNameProperty))
      .findFirst()
      .map(KeyValue::getValue)
      .map(AnyValue::getStringValue)
      .orElse(null);

    if (hasText(apiName) && hasText(apiVersion) && hasText(otelServiceName)) {
      return Optional.of(
        new ApiIdentifier(apiName, apiVersion, otelServiceName)
      );
    }

    return Optional.empty();
  }

  private record ApiIdentifier(
    String apiName,
    String apiVersion,
    String otelServiceName
  ) {}
}
