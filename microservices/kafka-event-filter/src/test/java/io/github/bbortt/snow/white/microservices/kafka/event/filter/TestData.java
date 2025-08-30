/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter;

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
public class TestData {

  public static final String OTEL_SERVICE_NAME = "kafka-event-filter";
  public static final String API_NAME = TestData.class.getSimpleName();
  public static final String API_VERSION = "1.2.3";

  public static final String TRACE_ID = "a7b2c9d4e8f1a6b3c5d7e9f2a4b6c8d0";
  public static final String SPAN_ID = "f3a7b2c9d4e8f1a6";

  @Builder.Default
  private final String serviceNameProperty = "service.name";

  @Builder.Default
  private final String apiNameProperty = "api.name";

  @Builder.Default
  private final String apiVersionProperty = "api.version";

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

  public final ResourceSpans resourceSpansWithResourceAttributes() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithScopeAttributes() {
    return ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder()
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(apiNameProperty)
                  .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(apiVersionProperty)
                  .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
              )
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(serviceNameProperty)
                  .setValue(
                    AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                  )
              )
          )
          .addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithSpanAttributes() {
    return ResourceSpans.newBuilder()
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(
          Span.newBuilder()
            .setTraceId(fromHex(TRACE_ID))
            .setSpanId(fromHex(SPAN_ID))
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(apiNameProperty)
                .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
            )
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(apiVersionProperty)
                .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
            )
            .addAttributes(
              KeyValue.newBuilder()
                .setKey(serviceNameProperty)
                .setValue(
                  AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME)
                )
            )
        )
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithAttributesOnEachLevel() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder().addAttributes(
          KeyValue.newBuilder()
            .setKey(apiNameProperty)
            .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
        )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder()
          .setScope(
            InstrumentationScope.newBuilder().addAttributes(
              KeyValue.newBuilder()
                .setKey(apiVersionProperty)
                .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
            )
          )
          .addSpans(
            Span.newBuilder()
              .setTraceId(fromHex("a7b2c9d4e8f1a6b3c5d7e9f2a4b6c8d0"))
              .setSpanId(fromHex("f3a7b2c9d4e8f1a6"))
              .addAttributes(
                KeyValue.newBuilder()
                  .setKey(serviceNameProperty)
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

  public final ResourceSpans resourceSpansWithoutApiName() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithoutApiVersion() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithoutOtelServiceName() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
      )
      .addScopeSpans(
        ScopeSpans.newBuilder().addSpans(Span.getDefaultInstance())
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithoutScopeSpans() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .build();
  }

  public final ResourceSpans resourceSpansWithoutSpans() {
    return ResourceSpans.newBuilder()
      .setResource(
        Resource.newBuilder()
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_NAME))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(apiVersionProperty)
              .setValue(AnyValue.newBuilder().setStringValue(API_VERSION))
          )
          .addAttributes(
            KeyValue.newBuilder()
              .setKey(serviceNameProperty)
              .setValue(AnyValue.newBuilder().setStringValue(OTEL_SERVICE_NAME))
          )
      )
      .addScopeSpans(ScopeSpans.getDefaultInstance())
      .build();
  }
}
