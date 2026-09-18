/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.lang.reflect.Field;
import java.util.Collection;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class ObjectUtils {

  /**
   * A {@code null} field is skipped and leaves the target untouched, but a non-null field —
   * including an empty collection — overwrites the target outright; this is what lets an empty
   * criteria set on update actually clear a gate's previously attached criteria.
   */
  @RealizesSw(SwTraceables.SW_010_UPDATE_MERGE_PATCH_EMPTY_CRITERIA_CLEARS)
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

        if (value instanceof Collection<?> newContents) {
          Object existingValue = targetField.get(target);
          if (existingValue instanceof Collection<?> existingCollection) {
            replaceContents(existingCollection, newContents);
            return;
          }
        }

        targetField.set(target, value);
      }
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new FailedToCopyFieldException(sourceField.getName(), e);
    }
  }

  /**
   * Mutates the existing collection in place (clear + addAll) instead of replacing the field
   * reference outright: a JPA-managed, {@code orphanRemoval = true} collection must stay the same
   * instance for Hibernate to track removed elements — swapping in a plain new collection makes
   * Hibernate lose track of it and throw at flush time.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void replaceContents(
    Collection<?> existingCollection,
    Collection<?> newContents
  ) {
    existingCollection.clear();
    ((Collection) existingCollection).addAll(newContents);
  }
}
