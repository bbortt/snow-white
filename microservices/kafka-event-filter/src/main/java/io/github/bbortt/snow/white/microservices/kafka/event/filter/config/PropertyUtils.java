/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.HashMap;
import java.util.Properties;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class PropertyUtils {

  static HashMap<String, String> propertiesToMap(Properties prop) {
    return prop
      .entrySet()
      .stream()
      .collect(
        toMap(
          e -> String.valueOf(e.getKey()),
          e -> String.valueOf(e.getValue()),
          (prev, next) -> next,
          HashMap::new
        )
      );
  }
}
