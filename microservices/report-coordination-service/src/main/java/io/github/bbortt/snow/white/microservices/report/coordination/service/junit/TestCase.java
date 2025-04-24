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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@With
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
@XmlAccessorType(XmlAccessType.FIELD)
public class TestCase {

  @XmlAttribute
  private String name;

  @XmlAttribute
  private String classname;

  @XmlAttribute
  @Builder.Default
  private String time = "0";

  @XmlElement
  private Failure failure;
}
