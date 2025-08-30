/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.protobuf.util.JsonFormat;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.exception.SerializationException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;

public class ExportTraceServiceRequestJsonSerializer
  implements Serializer<ExportTraceServiceRequest> {

  private static final Logger logger = getLogger("protobuf-jackson-bridge");

  private final JsonFormat.Printer printer = JsonFormat.printer();

  @Override
  public byte[] serialize(
    String topic,
    ExportTraceServiceRequest exportTraceServiceRequest
  ) {
    try {
      return printer.print(exportTraceServiceRequest).getBytes();
    } catch (Exception e) {
      logger.error("Error serializing protobuf JSON message", e);
      throw new SerializationException(
        "Error serializing protobuf JSON message",
        e
      );
    }
  }
}
