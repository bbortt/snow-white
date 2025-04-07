package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Field;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class ObjectUtils {

  public static void copyNonNullFields(Object source, Object target) {
    if (source == null || target == null) {
      throw new IllegalArgumentException("Source and target must not be null!");
    }

    Class<?> sourceClass = source.getClass();
    Class<?> targetClass = target.getClass();

    while (sourceClass != null && targetClass != null) {
      Field[] fields = sourceClass.getDeclaredFields();

      for (Field sourceField : fields) {
        try {
          sourceField.setAccessible(true);
          Object value = sourceField.get(source);
          if (value != null) {
            Field targetField;
            try {
              targetField = targetClass.getDeclaredField(sourceField.getName());
            } catch (NoSuchFieldException e) {
              continue; // target doesn't have this field
            }

            targetField.setAccessible(true);
            targetField.set(target, value);
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(
            "Failed to copy field: " + sourceField.getName(),
            e
          );
        }
      }

      sourceClass = sourceClass.getSuperclass();
      targetClass = targetClass.getSuperclass();
    }
  }
}
