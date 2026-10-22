/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads headers out of a plain map, so a produced {@code baggage} header can be round-tripped back
 * through OpenTelemetry's own propagator - which is what proves the receiving end would read it.
 */
final class MapGetter implements TextMapGetter<Map<String, String>> {

  @Override
  public Iterable<String> keys(Map<String, String> carrier) {
    return carrier.keySet();
  }

  @Override
  public @Nullable String get(
    @Nullable Map<String, String> carrier,
    String key
  ) {
    return carrier == null ? null : carrier.get(key);
  }
}
