/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.stream.StreamSupport;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

public final class Propagators {

  public static final TextMapGetter<Headers> KAFKA_HEADERS_GETTER =
    new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(Headers headers) {
        return () ->
          StreamSupport.stream(headers.spliterator(), false)
            .map(Header::key)
            .iterator();
      }

      @Override
      public String get(Headers headers, String key) {
        var header = headers.lastHeader(key);
        return header != null ? new String(header.value(), UTF_8) : null;
      }
    };

  public static final TextMapSetter<Headers> KAFKA_HEADERS_SETTER = (
    headers,
    key,
    value
  ) -> {
    headers.remove(key); // avoid duplicates on retries
    headers.add(new RecordHeader(key, value.getBytes(UTF_8)));
  };
}
