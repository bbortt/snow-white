/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream;

import static com.google.protobuf.ByteString.fromHex;
import static lombok.AccessLevel.PRIVATE;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor(access = PRIVATE)
public final class TestData {

  public static final String OTEL_SERVICE_NAME = "otel-event-filter-stream";
  public static final String API_NAME = TestData.class.getSimpleName();
  public static final String API_VERSION = "1.2.3";

  public static final String TRACE_ID = "a7b2c9d4e8f1a6b3c5d7e9f2a4b6c8d0";
  public static final String SPAN_ID = "f3a7b2c9d4e8f1a6";

  @Builder.Default
  private final String serviceNameAttributeKey = "service.name";

  @Builder.Default
  private final String apiNameAttributeKey = "api.name";

  @Builder.Default
  private final String apiVersionAttributeKey = "api.version";

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

  public ResourceSpans resourceSpansWithResourceAttributes() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public ResourceSpans resourceSpansWithScopeAttributes() {
    return ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(apiNameAttributeKey)
                  .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(apiVersionAttributeKey)
                  .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(serviceNameAttributeKey)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
          .addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public ResourceSpans resourceSpansWithSpanAttributes() {
    return ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(
          Span.newBuilder()
            .setTraceId(fromHex(TRACE_ID))
            .setSpanId(fromHex(SPAN_ID))
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(apiNameAttributeKey)
                .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
            )
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(apiVersionAttributeKey)
                .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
            )
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(serviceNameAttributeKey)
                .setValue(
                  AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                )
            )
        )
      )
      .build();
  }

  public ResourceSpans resourceSpansWithAttributesOnEachLevel() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder().addAttributes(
          KeyValue.newBuilder()
            .setKey(apiNameAttributeKey)
            .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
        )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder().addAttributes(
              KeyValue.newBuilder()
                .setKey(apiVersionAttributeKey)
                .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
            )
          )
          .addSpans(
            Span.newBuilder()
              .setTraceId(fromHex("a7b2c9d4e8f1a6b3c5d7e9f2a4b6c8d0"))
              .setSpanId(fromHex("f3a7b2c9d4e8f1a6"))
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(serviceNameAttributeKey)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
      )
      .build();
  }

  // -------------------------------------------
  // Invalid ResourceSpans
  // -------------------------------------------

  public ResourceSpans resourceSpansWithoutApiName() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public ResourceSpans resourceSpansWithoutApiVersion() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public ResourceSpans resourceSpansWithoutOtelServiceName() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public ResourceSpans resourceSpansWithoutScopeSpans() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .build();
  }

  public ResourceSpans resourceSpansWithoutSpans() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameAttributeKey)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(ScopeSpans.getDefaultInstance())
      .build();
  }
}
