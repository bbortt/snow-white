package io.github.bbortt.snow.white.rest.v1;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SpanResource implements SpanResourceApi {

  @Override
  public Long countRecordedSpans() {
    return 1L;
  }
}
