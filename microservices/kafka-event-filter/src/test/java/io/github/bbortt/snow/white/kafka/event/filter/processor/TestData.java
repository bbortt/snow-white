package io.github.bbortt.snow.white.kafka.event.filter.processor;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;

public class TestData {

  public static final String API_NAME_PROPERTY =
    TestData.class.getSimpleName() + ":apiNameProperty";
  public static final String API_VERSION_PROPERTY =
    TestData.class.getSimpleName() + ":apiVersionProperty";
  public static final String OTEL_SERVICE_NAME_PROPERTY =
    TestData.class.getSimpleName() + ":otelServiceNameProperty";

  public static final String API_NAME = TestData.class.getSimpleName();
  public static final String API_VERSION = "1.2.3";
  public static final String OTEL_SERVICE_NAME = "kafka-event-filter";

  // -------------------------------------------
  // Utility Method
  // -------------------------------------------

  public static ExportTraceServiceRequest wrapResourceSpans(
    ResourceSpans resourceSpans
  ) {
    return ExportTraceServiceRequest.newBuilder()
      .addResourceSpans(resourceSpans)
      .build();
  }

  // -------------------------------------------
  // Valid ResourceSpans
  // -------------------------------------------

  public static final ResourceSpans RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_VERSION_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(OTEL_SERVICE_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITH_SCOPE_ATTRIBUTES =
    ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(API_NAME_PROPERTY)
                  .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(API_VERSION_PROPERTY)
                  .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(OTEL_SERVICE_NAME_PROPERTY)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
          .addSpans(Span.getDefaultInstance())
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITH_SPAN_ATTRIBUTES =
    ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .addSpans(
            Span.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(API_NAME_PROPERTY)
                  .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(API_VERSION_PROPERTY)
                  .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(OTEL_SERVICE_NAME_PROPERTY)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITH_ATTRIBUTES_ON_EACH_LEVEL =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(API_VERSION_PROPERTY)
                  .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
              )
          )
          .addSpans(
            Span.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(OTEL_SERVICE_NAME_PROPERTY)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
      )
      .build();

  // -------------------------------------------
  // Invalid ResourceSpans
  // -------------------------------------------

  public static final ResourceSpans RESOURCE_SPANS_WITHOUT_API_NAME =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_VERSION_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(OTEL_SERVICE_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITHOUT_API_VERSION =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(OTEL_SERVICE_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITHOUT_OTEL_SERVICE_NAME =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITHOUT_SCOPE_SPANS =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_VERSION_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(OTEL_SERVICE_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .build();

  public static final ResourceSpans RESOURCE_SPANS_WITHOUT_SPANS =
    ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(API_VERSION_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(OTEL_SERVICE_NAME_PROPERTY)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(ScopeSpans.getDefaultInstance())
      .build();
}
