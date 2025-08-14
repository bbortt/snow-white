/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class Properties {

  static final String CALCULATION_ID = "calculationId";

  static final String SERVICE_NAME = "serviceName";
  static final String API_NAME = "apiName";
  static final String API_VERSION = "apiVersion";

  static final String DESCRIPTION = "description";
}
