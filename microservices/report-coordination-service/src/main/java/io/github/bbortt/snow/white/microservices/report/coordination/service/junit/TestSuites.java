/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@With
@Getter
@NoArgsConstructor
@JsonRootName("testsuites")
@AllArgsConstructor(access = PRIVATE)
public class TestSuites {

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
  private String time = "0";

  @JsonIgnore
  @With(PRIVATE)
  private transient Duration duration;

  @JacksonXmlProperty(isAttribute = true)
  private String timestamp;

  @JacksonXmlProperty(localName = "property")
  @JacksonXmlElementWrapper(localName = "properties")
  private Set<Property> properties;

  @JacksonXmlProperty(localName = "testsuite")
  @JacksonXmlElementWrapper(useWrapping = false)
  private Set<TestSuite> containedSuites = new LinkedHashSet<>();

  @Builder
  public TestSuites(String name, String timestamp, Set<Property> properties) {
    this.name = name;
    this.timestamp = timestamp;
    this.properties = properties;
  }

  public TestSuites withDuration(
    Duration duration,
    DurationFormatter formatter
  ) {
    this.duration = duration;
    this.time = formatter.toSecondsWithPrecision(duration);
    return this;
  }

  public void addAllTestSuite(Set<TestSuite> testSuites) {
    this.containedSuites.addAll(testSuites);
  }
}
