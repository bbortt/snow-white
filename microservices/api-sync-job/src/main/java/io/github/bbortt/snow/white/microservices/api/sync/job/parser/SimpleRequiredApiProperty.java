/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.parser;

public record SimpleRequiredApiProperty(String propertyName) implements
  ApiProperty {
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean isRequired() {
    return true;
  }
}
