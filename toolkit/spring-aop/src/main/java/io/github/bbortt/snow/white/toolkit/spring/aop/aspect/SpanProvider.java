package io.github.bbortt.snow.white.toolkit.spring.aop.aspect;

import io.opentelemetry.api.trace.Span;

class SpanProvider {

  Span getCurrentSpan() {
    return Span.current();
  }
}
