/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;

@With
@Getter
@Builder
@ToString
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class OpenApiInformation extends ApiInformation {

  private ApiType apiType = OPENAPI;
}
