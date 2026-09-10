/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nonnull;
import java.util.Set;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class PackageUtils {

  static boolean packageIsInIgnoreList(
    String packageName,
    @Nonnull Set<String> ignoringPackages
  ) {
    return ignoringPackages.stream().anyMatch(packageName::equals);
  }
}
