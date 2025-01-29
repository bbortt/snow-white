package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import io.opentelemetry.api.trace.Span;

class SpanProvider {

  Span getCurrentSpan() {
    return Span.current();
  }
}
