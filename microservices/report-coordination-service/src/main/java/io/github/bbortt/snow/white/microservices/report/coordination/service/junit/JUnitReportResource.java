/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import org.springframework.core.io.ByteArrayResource;

class JUnitReportResource extends ByteArrayResource {

  @VisibleForTesting
  static final String FILENAME = "snow-white-junit.xml";

  JUnitReportResource(byte[] byteArray) {
    super(byteArray);
  }

  @Override
  public String getFilename() {
    return FILENAME;
  }
}
