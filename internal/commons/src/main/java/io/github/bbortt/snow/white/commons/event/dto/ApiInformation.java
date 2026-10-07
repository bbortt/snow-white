/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static lombok.AccessLevel.PROTECTED;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@With
@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
public final class ApiInformation {

  @NonNull
  private String serviceName;

  @NonNull
  private String apiName;

  @Nullable
  private String apiVersion;

  @NonNull
  private ApiType apiType;
}
