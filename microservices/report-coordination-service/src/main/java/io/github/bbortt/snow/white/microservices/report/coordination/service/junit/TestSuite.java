/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@With
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
public class TestSuite {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  private String time = "0";

  @Builder.Default
  @JacksonXmlProperty(localName = "testcase")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<TestCase> testCases = new ArrayList<>();

  public TestSuite(String name) {
    this.name = name;
    this.time = "0";
    this.testCases = new ArrayList<>();
  }

  public void addTestCase(TestCase testCase) {
    testCases.add(testCase);
  }
}
