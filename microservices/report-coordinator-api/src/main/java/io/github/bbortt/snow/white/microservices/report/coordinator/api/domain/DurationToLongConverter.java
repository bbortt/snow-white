/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class DurationToLongConverter
  implements AttributeConverter<Duration, Long>
{

  @Override
  @Nullable
  public Long convertToDatabaseColumn(@Nullable Duration duration) {
    return duration == null ? null : duration.toNanos();
  }

  @Override
  @Nullable
  public Duration convertToEntityAttribute(@Nullable Long value) {
    return value == null ? null : Duration.ofNanos(value);
  }
}
