/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@Builder
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class QualityGateCalculationRequestEvent {

  @Nonnull
  private ApiInformation apiInformation;

  @Nonnull
  private String lookbackWindow;
}
