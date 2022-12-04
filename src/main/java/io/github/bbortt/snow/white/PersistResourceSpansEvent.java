package io.github.bbortt.snow.white;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.util.List;

public class PersistResourceSpansEvent {

  public static final String EVENT_NAME = "persist-resource-spans";

  private final List<ResourceSpans> resourceSpans;

  public PersistResourceSpansEvent(List<ResourceSpans> resourceSpans) {
    this.resourceSpans = resourceSpans;
  }
}
