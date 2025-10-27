/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@With
@Getter
@AllArgsConstructor(access = PRIVATE)
public class TestSuite {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private Long tests;

  @JacksonXmlProperty(isAttribute = true)
  private Long failures;

  @JacksonXmlProperty(isAttribute = true)
  private Long errors = 0L;

  @JacksonXmlProperty(isAttribute = true)
  private Long skipped = 0L;

  @JacksonXmlProperty(isAttribute = true)
  private Long assertions;

  @With(PRIVATE)
  @JacksonXmlProperty(isAttribute = true)
  private String time;

  @JsonIgnore
  @With(PRIVATE)
  private transient Duration duration;

  @JacksonXmlProperty(isAttribute = true)
  private String timestamp;

  @JacksonXmlProperty(localName = "property")
  @JacksonXmlElementWrapper(localName = "properties")
  private Set<Property> properties;

  @JacksonXmlProperty(localName = "testcase")
  @JacksonXmlElementWrapper(useWrapping = false)
  private Set<TestCase> testCases = new LinkedHashSet<>();

  @Builder
  public TestSuite(String name, Set<Property> properties) {
    this.name = name;
    this.time = "0";
    this.properties = properties;
  }

  public TestSuite withDuration(
    Duration duration,
    DurationFormatter formatter
  ) {
    this.duration = duration;
    this.time = formatter.toSecondsWithPrecision(duration);
    return this;
  }

  public void addAllTestCases(Set<TestCase> testCases) {
    this.testCases.addAll(testCases);
  }
}
