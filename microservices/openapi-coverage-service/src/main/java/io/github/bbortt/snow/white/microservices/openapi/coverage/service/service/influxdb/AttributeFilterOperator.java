package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;

@VisibleForTesting
public enum AttributeFilterOperator {
  STRING_EQUALS("==");

  private final String fluxComparator;

  AttributeFilterOperator(String fluxComparator) {
    this.fluxComparator = fluxComparator;
  }

  public String toFluxString() {
    return fluxComparator;
  }
}
