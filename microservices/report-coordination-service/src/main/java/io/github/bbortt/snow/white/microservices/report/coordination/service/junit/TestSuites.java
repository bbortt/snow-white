/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
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
@XmlRootElement(name = "testsuites")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestSuites {

  @XmlAttribute
  private String name;

  @XmlAttribute
  private Long tests;

  @XmlAttribute
  private Long failures;

  @XmlAttribute
  @Builder.Default
  private Long errors = 0L;

  @XmlAttribute
  @Builder.Default
  private Long skipped = 0L;

  @XmlAttribute
  @Builder.Default
  private String time = "0";

  @XmlAttribute
  private String timestamp;

  @Builder.Default
  @XmlElement(name = "testsuite")
  private List<TestSuite> containedSuites = new ArrayList<>();

  public void addTestSuite(TestSuite suite) {
    this.containedSuites.add(suite);
  }
}
