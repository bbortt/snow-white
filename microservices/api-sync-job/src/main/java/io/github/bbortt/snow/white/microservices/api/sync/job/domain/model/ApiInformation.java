/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
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
public class ApiInformation {

  private String title;
  private String version;

  private String sourceUrl;

  private String name;
  private String serviceName;

  private ApiType apiType;

  @Builder.Default
  private ApiLoadStatus loadStatus = UNLOADED;
}
