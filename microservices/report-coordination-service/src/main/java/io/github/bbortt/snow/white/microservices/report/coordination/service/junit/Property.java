/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

@With
@Getter
@Builder(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class Property {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private String value;

  /**
   * For {@code <description>} or other text-only tags.
   * Will be serialized as text content of the XML element.
   * For example:
   * <pre>
   * {@code <property name="description">This is a test property</property>}
   * </pre>
   */
  @JacksonXmlText
  private String text;

  static Property property(String name, String value) {
    return Property.builder().name(name).value(value).build();
  }
}
