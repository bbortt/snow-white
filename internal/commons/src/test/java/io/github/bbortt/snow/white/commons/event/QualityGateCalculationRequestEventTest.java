/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class QualityGateCalculationRequestEventTest {

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(QualityGateCalculationRequestEvent.class)
      .suppress(NONFINAL_FIELDS)
      .verify();
  }
}
