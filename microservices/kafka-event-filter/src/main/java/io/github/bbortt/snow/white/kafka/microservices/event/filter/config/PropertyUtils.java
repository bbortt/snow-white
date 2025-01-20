package io.github.bbortt.snow.white.kafka.microservices.event.filter.config;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Properties;

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
