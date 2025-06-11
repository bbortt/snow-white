/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JacksonXmlRootElement(localName = "testsuites")
public class TestSuites {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private Long tests;

  @JacksonXmlProperty(isAttribute = true)
  private Long failures;

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  private Long errors = 0L;

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  private Long skipped = 0L;

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  private String time = "0";

  @JacksonXmlProperty(isAttribute = true)
  private String timestamp;

  @Builder.Default
  @JacksonXmlProperty(localName = "testsuite")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<TestSuite> containedSuites = new ArrayList<>();

  public void addTestSuite(TestSuite suite) {
    this.containedSuites.add(suite);
  }
}
