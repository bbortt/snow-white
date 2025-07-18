/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

public class JUnitReportCreationException extends Exception {

  public JUnitReportCreationException(Throwable cause) {
    super("Failed to create JUnit report!", cause);
  }
}
