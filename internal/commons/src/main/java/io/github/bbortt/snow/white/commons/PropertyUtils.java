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
}
