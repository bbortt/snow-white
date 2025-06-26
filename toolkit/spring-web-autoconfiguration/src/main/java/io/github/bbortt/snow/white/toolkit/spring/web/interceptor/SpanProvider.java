/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import io.opentelemetry.api.trace.Span;

class SpanProvider {

  Span getCurrentSpan() {
    return Span.current();
  }
}
