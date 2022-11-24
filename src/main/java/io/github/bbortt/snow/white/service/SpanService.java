package io.github.bbortt.snow.white.service;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpanService {

  @Modifying
  @Transactional
  public void persistAll(List<ResourceSpans> resourceSpansList) {}
}
