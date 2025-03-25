package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb;

enum AttributeFilterOperator {
  STRING_EQUALS("==");

  private final String fluxComparator;

  AttributeFilterOperator(String fluxComparator) {
    this.fluxComparator = fluxComparator;
  }

  public String toFluxString() {
    return fluxComparator;
  }
}
