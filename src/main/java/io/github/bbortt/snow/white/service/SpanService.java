package io.github.bbortt.snow.white.service;

import io.github.bbortt.snow.white.PersistResourceSpansEvent;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.EventBus;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SpanService {

  private static final Logger logger = LoggerFactory.getLogger(SpanService.class);

  @Inject
  SpecificationService specificationService;

  @Inject
  EventBus eventBus;

  public Uni<Void> persistAll(List<ResourceSpans> resourceSpansList) {
    logger.debug("Received list of {} spans for persisting", resourceSpansList.size());

    List<ResourceSpans> filteredResourceSpans = resourceSpansList
      .stream()
      .filter(this::specificationServiceContainsOpenAPISpecificationForServiceExists)
      .toList();

    logger.trace("{} of {} resource spans will be persisted...", filteredResourceSpans.size(), resourceSpansList.size());

    eventBus.send(PersistResourceSpansEvent.EVENT_NAME, new PersistResourceSpansEvent(resourceSpansList));

    return Uni.createFrom().voidItem();
  }

  @Transactional
  @ConsumeEvent
  public void persistResourceSpans(PersistResourceSpansEvent persistResourceSpansEvent) {}

  private boolean specificationServiceContainsOpenAPISpecificationForServiceExists(ResourceSpans resourceSpans) {
    if (logger.isDebugEnabled()) {
      logger.debug("Filtering resources spans for resource: {}", resourceSpans.getResource());
    }

    Optional<String> serviceName = resourceSpans
      .getResource()
      .getAttributesList()
      .stream()
      .map(KeyValue::getKey)
      .filter(key -> key.equals(ResourceAttributes.SERVICE_NAME.getKey()))
      .findFirst();

    boolean includeResourceSpans = serviceName.isPresent() && specificationService.openAPISpecificationForServiceExists(serviceName.get());

    if (logger.isTraceEnabled()) {
      logger.trace("Resource spans will be {}", includeResourceSpans ? "included" : "dropped");
    }

    return includeResourceSpans;
  }
}
