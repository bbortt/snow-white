package io.github.bbortt.snow.white.service;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpanService {

  private static final Logger logger = LoggerFactory.getLogger(SpanService.class);

  @Modifying
  @Transactional
  public void persistAll(List<ResourceSpans> resourceSpansList) {
    logger.info("Received list of {} spans", resourceSpansList.size());

    if (resourceSpansList.size() >= 1) {
      logger.info("First span is: {}", resourceSpansList.get(0));
    }
  }
}
