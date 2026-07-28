/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Ping request payload")
public class PingRequest {

  @Schema(
    description = "Message to include with the ping",
    example = "Hello server!"
  )
  private String message;

  @NotNull
  @Schema(
    description = "Time of the ping request",
    example = "2025-02-25T12:00:00Z",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private OffsetDateTime timestamp;
}
