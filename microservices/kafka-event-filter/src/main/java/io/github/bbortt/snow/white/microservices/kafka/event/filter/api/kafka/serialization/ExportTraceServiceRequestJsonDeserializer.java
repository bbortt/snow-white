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
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;

public class ExportTraceServiceRequestJsonDeserializer
  implements Deserializer<ExportTraceServiceRequest> {

  private static final Logger logger = getLogger("protobuf-jackson-bridge");

  private static final JsonFormat.Parser parser =
    JsonFormat.parser().ignoringUnknownFields();

  @Override
  public ExportTraceServiceRequest deserialize(String topic, byte[] bytes) {
    try {
      var builder = ExportTraceServiceRequest.newBuilder();
      parser.merge(new String(bytes), builder);
      return builder.build();
    } catch (Exception e) {
      logger.error("Error deserializing protobuf JSON message", e);
      throw new SerializationException(
        "Error deserializing protobuf JSON message",
        e
      );
    }
  }
}
