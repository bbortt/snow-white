/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

@Converter(autoApply = true)
public class DurationToLongConverter
  implements AttributeConverter<Duration, Long> {

  @Override
  public Long convertToDatabaseColumn(Duration duration) {
    return duration == null ? null : duration.toNanos();
  }

  @Override
  public Duration convertToEntityAttribute(Long value) {
    return value == null ? null : Duration.ofNanos(value);
  }
}
