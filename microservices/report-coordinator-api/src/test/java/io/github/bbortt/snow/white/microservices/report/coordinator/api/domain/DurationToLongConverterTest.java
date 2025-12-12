/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DurationToLongConverterTest {

  private DurationToLongConverter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DurationToLongConverter();
  }

  @Test
  void convertToDatabaseColumn_shouldConvertDurationToMillis() {
    Duration duration = Duration.ofMinutes(5);

    Long dbValue = fixture.convertToDatabaseColumn(duration);

    assertThat(dbValue).isNotNull().isEqualTo(duration.toNanos());
  }

  @Test
  void convertToDatabaseColumn_shouldHandleNull() {
    assertThat(fixture.convertToDatabaseColumn(null)).isNull();
  }

  @Test
  void convertToEntityAttribute_shouldConvertMillisToDuration() {
    Long millis = 12345L;

    Duration duration = fixture.convertToEntityAttribute(millis);

    assertThat(duration).isNotNull().isEqualTo(Duration.ofNanos(12345));
  }

  @Test
  void convertToEntityAttribute_shouldHandleNull() {
    assertThat(fixture.convertToEntityAttribute(null));
  }
}
