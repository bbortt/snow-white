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
@Schema(description = "Success response")
public class SuccessResponse {

  @NotNull
  @Schema(
    description = "Indicates if the operation was successful",
    example = "true",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Boolean success;

  @Schema(
    description = "Success message",
    example = "Operation completed successfully"
  )
  private String message;
}
