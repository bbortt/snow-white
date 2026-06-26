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
@Schema(description = "Pong response")
public class PongResponse {

  @NotNull
  @Schema(
    description = "Response message",
    example = "Pong! Hello server!",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String message;

  @NotNull
  @Schema(
    description = "Time of the response",
    example = "2025-02-25T12:00:01Z",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private OffsetDateTime timestamp;

  @NotNull
  @Schema(
    description = "Response time in milliseconds",
    example = "42",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Integer latency;
}
