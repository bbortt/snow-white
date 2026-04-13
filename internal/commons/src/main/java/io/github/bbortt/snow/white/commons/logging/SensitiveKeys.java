/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class SensitiveKeys {

  public static final Set<String> KEYS = Set.of(
    "token",
    "accessToken",
    "password",
    "secret",
    "apiKey"
  );
}
