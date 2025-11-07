/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class XmlMapperConfigurationTest {

  private XmlMapperConfiguration fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new XmlMapperConfiguration();
  }

  @Nested
  class XmlMapper {

    @Test
    void shouldReturnJacksonXmlMapper() {
      assertThat(fixture.xmlMapper()).isInstanceOf(
        tools.jackson.dataformat.xml.XmlMapper.class
      );
    }
  }
}
