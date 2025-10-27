/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.redis;

import static io.micrometer.common.util.StringUtils.isEmpty;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class RedisHashUtils {

  public static String generateRedisApiInformationId(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    if (isEmpty(otelServiceName) || isEmpty(apiName) || isEmpty(apiVersion)) {
      throw new IllegalArgumentException(
        "ID cannot be constructed from nullish values!"
      );
    }

    return format("%s:%s:%s", otelServiceName, apiName, apiVersion);
  }
}
