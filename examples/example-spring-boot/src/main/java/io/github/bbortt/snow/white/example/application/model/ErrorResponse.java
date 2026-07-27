/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Error response")
public class ErrorResponse {

  @NotNull
  @Schema(
    description = "Error message",
    example = "Invalid request parameters",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String error;

  @NotNull
  @Schema(
    description = "Error code",
    example = "INVALID_PARAMS",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String code;

  @Schema(description = "Additional error details")
  private Object details;
}
