package io.github.bbortt.snow.white.service;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.smallrye.mutiny.Uni;
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

  @Transactional
  public Uni<Void> persistAll(List<ResourceSpans> resourceSpansList) {
    logger.debug("Received list of {} spans", resourceSpansList.size());

    List<ResourceSpans> filteredResourceSpans = resourceSpansList
      .stream()
      .filter(this::specificationServiceContainsOpenAPISpecificationForServiceExists)
      .toList();

    logger.info("{} of {} resource spans will be persisted...", filteredResourceSpans.size(), resourceSpansList.size());

    return Uni.createFrom().voidItem();
  }

  private boolean specificationServiceContainsOpenAPISpecificationForServiceExists(ResourceSpans resourceSpans) {
    List<KeyValue> attributesList = resourceSpans.getResource().getAttributesList();
    Optional<String> serviceName = attributesList
      .stream()
      .map(KeyValue::getKey)
      .filter(key -> key.equals(ResourceAttributes.SERVICE_NAME.getKey()))
      .findFirst();
    return serviceName.isPresent() && specificationService.openAPISpecificationForServiceExists(serviceName.get());
  }
}
