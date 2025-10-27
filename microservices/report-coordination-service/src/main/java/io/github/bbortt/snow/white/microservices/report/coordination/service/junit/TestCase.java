/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
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
public class TestCase {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private String classname;

  @With(PRIVATE)
  @JacksonXmlProperty(isAttribute = true)
  private String time = "0";

  @JsonIgnore
  @With(PRIVATE)
  private transient Duration duration;

  @JacksonXmlProperty
  private Failure failure;

  @JacksonXmlProperty
  private Skipped skipped;

  @JacksonXmlProperty(localName = "property")
  @JacksonXmlElementWrapper(localName = "properties")
  private Set<Property> properties;

  @Builder
  public TestCase(String name, String classname, Set<Property> properties) {
    this.name = name;
    this.classname = classname;
    this.properties = properties;
  }

  public TestCase withDuration(Duration duration, DurationFormatter formatter) {
    this.duration = duration;
    this.time = formatter.toSecondsWithPrecision(duration);
    return this;
  }
}
