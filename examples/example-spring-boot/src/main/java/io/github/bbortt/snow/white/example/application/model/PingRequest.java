/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * `@Builder` replaces the no-argument constructor `@Data` would otherwise generate with an
 * all-argument one, leaving Jackson no way to instantiate the type - every `POST /pong` answered
 * with a 500 ("no Creators, like default constructor, exist"). The no-argument constructor plus the
 * setters `@Data` generates give it one back, and `@Builder` keeps needing the all-argument one.
 *
 * Lombok's `@Jacksonized` does not help here: it emits the Jackson 2 `@JsonDeserialize`, and Spring
 * Boot 4 deserializes with Jackson 3 (`tools.jackson`), which ignores it.
 *
 * Only the request model needs this. The responses are exclusively serialized.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
