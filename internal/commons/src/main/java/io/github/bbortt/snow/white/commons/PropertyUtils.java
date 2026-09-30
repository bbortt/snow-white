/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class PropertyUtils {

  public static void assertRequiredProperties(Map<String, String> properties) {
    var emptyFields = properties
      .entrySet()
      .stream()
      .map(field ->
        Map.entry(
          field.getKey(),
          Optional.ofNullable(field.getValue()).orElse("")
        )
      )
      .filter(field -> field.getValue().trim().isEmpty())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!emptyFields.isEmpty()) {
      throw new IllegalArgumentException(
        format(
          "All properties must be configured - missing: %s.",
          emptyFields.keySet()
        )
      );
    }
  }

  /**
   * Asserts that a bound property is at least {@code 1}.
   *
   * <p>Knobs sizing an executor or a queue are rejected below one deep inside
   * the constructor they are handed to, with an exception naming neither the
   * value nor the property it came from. Refusing them here names both.
   */
  public static void assertPositiveProperty(
    String propertyName,
    Integer value
  ) {
    if (value < 1) {
      throw new IllegalArgumentException(
        format(
          "Property '%s' must be greater than 0, but was: %s!",
          propertyName,
          value
        )
      );
    }
  }
}
