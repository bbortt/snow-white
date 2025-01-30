package io.github.bbortt.snow.white.kafka.microservices.event.filter.processor;

import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.kafka.microservices.event.filter.service.CachingService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExportTraceServiceRequestFilter {

  private final CachingService cachingService;

  private final String apiNameProperty;
  private final String apiVersionProperty;
  private final String otelServiceNameProperty;

  public ExportTraceServiceRequestFilter(
    CachingService cachingService,
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    this.cachingService = cachingService;

    var filteringProperties = kafkaEventFilterProperties.getFiltering();
    this.apiNameProperty = filteringProperties.getApiNameProperty();
    this.apiVersionProperty = filteringProperties.getApiVersionProperty();
    this.otelServiceNameProperty =
      filteringProperties.getOtelServiceNameProperty();

    logger.info("Filter is in place: {}", filteringProperties);
  }

  public @Nonnull ExportTraceServiceRequest filterUnknownSpecifications(
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

  private @Nonnull ResourceSpans filterResourceSpansDropAllOfUnknownSpecifications(
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

  private @Nonnull ScopeSpans filterScopeSpansDropAllOfUnknownSpecifications(
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
    var identifier3 = extractApiIdentifyingAttributes(
      Stream.of(
        resourceAttributes.stream(),
        scopeAttributes.stream(),
        spanAttributes.stream()
      )
        .flatMap(identity())
        .toList()
    );

    if (identifier3.isPresent() && apiIsKnownToSnowWhite(identifier3.get())) {
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
      .filter(attribute -> attribute.getKey().equals(otelServiceNameProperty))
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
