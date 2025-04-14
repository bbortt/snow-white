/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static org.apache.commons.lang3.StringUtils.isBlank;

record OpenApiInformation(
  String apiName,
  String apiVersion,
  String serviceName
) {
  boolean isIncomplete() {
    return isBlank(apiName) || isBlank(apiVersion) || isBlank(serviceName);
  }
}
