/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import java.util.Set;

public final class SensitiveKeys {

  public static final Set<String> KEYS = Set.of(
    "token",
    "accessToken",
    "password",
    "secret",
    "apiKey"
  );
}
