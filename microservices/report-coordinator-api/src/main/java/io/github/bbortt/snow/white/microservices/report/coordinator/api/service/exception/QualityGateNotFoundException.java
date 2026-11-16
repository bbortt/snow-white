/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception;

import static java.lang.String.format;

public class QualityGateNotFoundException extends Exception {

  private QualityGateNotFoundException(String message) {
    super(message);
  }

  /** The rejection this service writes itself, when the gate's owner said nothing more about it. */
  public static QualityGateNotFoundException forGateName(
    String qualityGateConfigName
  ) {
    return new QualityGateNotFoundException(
      format(
        "No Quality-Gate configuration with ID '%s' exists!",
        qualityGateConfigName
      )
    );
  }

  /**
   * The Quality-Gate API's own rejection, surfaced verbatim. The gates are that service's to own,
   * so why a name could not have matched — a gate file defining none, as opposed to a typo — is
   * its to explain; repeating the sentence here would put it in the one service that cannot see the
   * file.
   */
  public static QualityGateNotFoundException surfacing(String ownerMessage) {
    return new QualityGateNotFoundException(ownerMessage);
  }
}
