/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.kafka;

import static io.github.bbortt.snow.white.commons.kafka.OtelPropagators.KAFKA_HEADERS_GETTER;
import static io.github.bbortt.snow.white.commons.kafka.OtelPropagators.KAFKA_HEADERS_SETTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OtelPropagatorsTest {

  @Nested
  class KafkaHeadersGetterTest {

    @Test
    void shouldReturnAllHeaderKeys() {
      Headers headers = new RecordHeaders();
      headers.add("traceparent", "value-a".getBytes(UTF_8));
      headers.add("tracestate", "value-b".getBytes(UTF_8));

      var keys = KAFKA_HEADERS_GETTER.keys(headers);

      assertThat(keys).containsExactlyInAnyOrder("traceparent", "tracestate");
    }

    @Test
    void shouldReturnEmptyIterator_whenHeadersAreEmpty() {
      Headers headers = new RecordHeaders();

      var keys = KAFKA_HEADERS_GETTER.keys(headers);

      assertThat(keys).isEmpty();
    }

    @Test
    void shouldReturnHeaderValue_whenHeaderExists() {
      Headers headers = new RecordHeaders();
      headers.add("traceparent", "00-abc123-def456-01".getBytes(UTF_8));

      String value = KAFKA_HEADERS_GETTER.get(headers, "traceparent");

      assertThat(value).isEqualTo("00-abc123-def456-01");
    }

    @Test
    void shouldReturnLastValue_whenHeaderIsDuplicated() {
      Headers headers = new RecordHeaders();
      headers.add("traceparent", "first".getBytes(UTF_8));
      headers.add("traceparent", "second".getBytes(UTF_8));

      String value = KAFKA_HEADERS_GETTER.get(headers, "traceparent");

      assertThat(value).isEqualTo("second");
    }

    @Test
    void shouldReturnNull_whenHeaderDoesNotExist() {
      Headers headers = new RecordHeaders();

      String value = KAFKA_HEADERS_GETTER.get(headers, "traceparent");

      assertThat(value).isNull();
    }

    @Test
    void shouldReturnNull_whenHeadersAreEmpty() {
      Headers headers = new RecordHeaders();

      String value = KAFKA_HEADERS_GETTER.get(headers, "any-key");

      assertThat(value).isNull();
    }

    @Test
    void shouldReturnNull_whenHeadersIsNull() {
      String value = KAFKA_HEADERS_GETTER.get(null, "any-key");

      assertThat(value).isNull();
    }
  }

  @Nested
  class KafkaHeadersSetterTest {

    @Test
    void shouldAddHeaderWithEncodedValue() {
      Headers headers = new RecordHeaders();

      KAFKA_HEADERS_SETTER.set(headers, "traceparent", "00-abc-01");

      var header = headers.lastHeader("traceparent");
      assertThat(header).isNotNull();
      assertThat(new String(header.value(), UTF_8)).isEqualTo("00-abc-01");
    }

    @Test
    void shouldRemoveDuplicates_beforeAdding() {
      Headers headers = new RecordHeaders();
      headers.add("traceparent", "stale-value".getBytes(UTF_8));

      KAFKA_HEADERS_SETTER.set(headers, "traceparent", "fresh-value");

      // only one header with the key must survive
      var remaining = headers.headers("traceparent");
      assertThat(remaining).hasSize(1);
      assertThat(
        new String(remaining.iterator().next().value(), UTF_8)
      ).isEqualTo("fresh-value");
    }

    @Test
    void shouldNotAffectOtherHeaders() {
      Headers headers = new RecordHeaders();
      headers.add("tracestate", "vendor=xyz".getBytes(UTF_8));

      KAFKA_HEADERS_SETTER.set(headers, "traceparent", "00-abc-01");

      assertThat(headers.lastHeader("tracestate")).isNotNull();
      assertThat(
        new String(headers.lastHeader("tracestate").value(), UTF_8)
      ).isEqualTo("vendor=xyz");
    }

    @Test
    void shouldHandleMultipleDistinctKeys() {
      Headers headers = new RecordHeaders();

      KAFKA_HEADERS_SETTER.set(headers, "traceparent", "00-abc-01");
      KAFKA_HEADERS_SETTER.set(headers, "tracestate", "vendor=xyz");

      assertThat(
        new String(headers.lastHeader("traceparent").value(), UTF_8)
      ).isEqualTo("00-abc-01");
      assertThat(
        new String(headers.lastHeader("tracestate").value(), UTF_8)
      ).isEqualTo("vendor=xyz");
    }

    @Test
    void shouldReturnImmediatelyIfHeadersAreNull() {
      assertDoesNotThrow(() -> KAFKA_HEADERS_SETTER.set(null, "key", "value"));
    }
  }
}
