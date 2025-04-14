/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb;

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
