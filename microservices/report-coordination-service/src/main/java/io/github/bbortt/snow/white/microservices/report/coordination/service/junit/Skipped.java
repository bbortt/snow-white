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

@With
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
public class Skipped {

  @JacksonXmlProperty(isAttribute = true)
  private String message;
}
