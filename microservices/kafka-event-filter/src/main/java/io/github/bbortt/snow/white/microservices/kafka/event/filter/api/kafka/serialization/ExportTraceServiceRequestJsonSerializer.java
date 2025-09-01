/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization;

import com.google.protobuf.util.JsonFormat;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.exception.SerializationException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.apache.kafka.common.serialization.Serializer;

public class ExportTraceServiceRequestJsonSerializer
  implements Serializer<ExportTraceServiceRequest> {

  private final JsonFormat.Printer printer = JsonFormat.printer();

  @Override
  public byte[] serialize(
    String topic,
    ExportTraceServiceRequest exportTraceServiceRequest
  ) {
    try {
      return printer.print(exportTraceServiceRequest).getBytes();
    } catch (Exception e) {
      throw new SerializationException(
        "Error serializing protobuf JSON message",
        e
      );
    }
  }
}
