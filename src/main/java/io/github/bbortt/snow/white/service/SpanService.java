package io.github.bbortt.snow.white.service;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SpanService {

  private static final Logger logger = LoggerFactory.getLogger(SpanService.class);

  @Transactional
  public Uni<Void> persistAll(List<ResourceSpans> resourceSpansList) {
    logger.info("Received list of {} spans", resourceSpansList.size());

    if (resourceSpansList.size() >= 1) {
      logger.info("First span is: {}", resourceSpansList.get(0));
    }

    return Uni.createFrom().voidItem();
  }
}
