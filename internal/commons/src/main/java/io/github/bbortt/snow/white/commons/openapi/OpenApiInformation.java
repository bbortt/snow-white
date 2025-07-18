/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.openapi;

import static org.springframework.util.StringUtils.hasText;

public record OpenApiInformation(
  String apiName,
  String apiVersion,
  String serviceName
) {
  public boolean isIncomplete() {
    return !hasText(apiName) || !hasText(apiVersion) || !hasText(serviceName);
  }
}
