/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ApiInformationTest {

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(ApiInformation.class)
      .suppress(NONFINAL_FIELDS)
      .verify();
  }
}
