/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

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
        copyField(source, target, sourceField, targetClass);
      }

      sourceClass = sourceClass.getSuperclass();
      targetClass = targetClass.getSuperclass();
    }
  }

  private static void copyField(
    Object source,
    Object target,
    Field sourceField,
    Class<?> targetClass
  ) {
    try {
      sourceField.setAccessible(true);
      Object value = sourceField.get(source);
      if (value != null) {
        Field targetField = targetClass.getDeclaredField(sourceField.getName());

        targetField.setAccessible(true);
        targetField.set(target, value);
      }
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new FailedToCopyFieldException(sourceField.getName(), e);
    }
  }
}
