/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.stream.StreamSupport;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PRIVATE)
public final class OtelPropagators {

  public static final TextMapGetter<Headers> KAFKA_HEADERS_GETTER =
    new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(@NonNull Headers headers) {
        return () ->
          StreamSupport.stream(headers.spliterator(), false)
            .map(Header::key)
            .iterator();
      }

      @Override
      public @Nullable String get(@Nullable Headers headers, String key) {
        if (isNull(headers)) {
          return null;
        }

        var header = headers.lastHeader(key);
        return header != null ? new String(header.value(), UTF_8) : null;
      }
    };

  public static final TextMapSetter<Headers> KAFKA_HEADERS_SETTER = (
    headers,
    key,
    value
  ) -> {
    if (isNull(headers)) {
      return;
    }

    headers.remove(key);
    headers.add(new RecordHeader(key, value.getBytes(UTF_8)));
  };
}
