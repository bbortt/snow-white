/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static tools.jackson.dataformat.xml.XmlMapper.xmlBuilder;
import static tools.jackson.dataformat.xml.XmlWriteFeature.WRITE_STANDALONE_YES_TO_XML_DECLARATION;
import static tools.jackson.dataformat.xml.XmlWriteFeature.WRITE_XML_DECLARATION;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.dataformat.xml.XmlMapper;

@Configuration
public class XmlMapperConfiguration {

  @Bean
  public XmlMapper xmlMapper() {
    return xmlBuilder()
      .changeDefaultPropertyInclusion(incl ->
        incl.withValueInclusion(NON_EMPTY)
      )
      .enable(INDENT_OUTPUT)
      .enable(WRITE_XML_DECLARATION)
      .enable(WRITE_STANDALONE_YES_TO_XML_DECLARATION)
      .build();
  }
}
