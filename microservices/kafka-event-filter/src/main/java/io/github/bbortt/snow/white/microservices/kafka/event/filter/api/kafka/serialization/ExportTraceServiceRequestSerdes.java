/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.kafka.common.serialization.Serdes.serdeFrom;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.serialization.Serde;

@NoArgsConstructor(access = PRIVATE)
public class ExportTraceServiceRequestSerdes {

  public static Serde<ExportTraceServiceRequest> JsonSerde() {
    return serdeFrom(
      new ExportTraceServiceRequestJsonSerializer(),
      new ExportTraceServiceRequestJsonDeserializer()
    );
  }
}
