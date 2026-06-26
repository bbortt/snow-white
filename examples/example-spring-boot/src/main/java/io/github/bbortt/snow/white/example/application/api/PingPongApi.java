/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.api;

import static java.lang.Boolean.TRUE;
import static java.time.OffsetDateTime.now;

import io.github.bbortt.snow.white.example.application.model.ErrorResponse;
import io.github.bbortt.snow.white.example.application.model.PingRequest;
import io.github.bbortt.snow.white.example.application.model.PongResponse;
import io.github.bbortt.snow.white.example.application.model.SuccessResponse;
import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "ping-pong", description = "Ping-pong operations")
public class PingPongApi {

  @GetMapping("/ping")
  @Operation(
    summary = "Send a ping request",
    description = "Returns a pong response",
    operationId = "getPing"
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Successful ping",
      content = @Content(schema = @Schema(implementation = PongResponse.class))
    ),
    @ApiResponse(
      responseCode = "429",
      description = "Too many requests",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
  })
  @SnowWhiteInformation(
    serviceName = "example-spring-boot",
    apiName = "ping-pong",
    apiVersion = "1.0.0",
    operationId = "getPing"
  )
  public ResponseEntity<PongResponse> getPing(
    @Parameter(
      description = "Optional message to include with the ping"
    ) @RequestParam(required = false) String message
  ) {
    return ResponseEntity.ok(
      PongResponse.builder()
        .message(message)
        .timestamp(now())
        .latency(0)
        .build()
    );
  }

  @PostMapping("/pong")
  @Operation(
    summary = "Send a pong request",
    description = "Send a pong directly (usually in response to a ping)",
    operationId = "postPong"
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "201",
      description = "Pong successfully sent",
      content = @Content(
        schema = @Schema(implementation = SuccessResponse.class)
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
  })
  @SnowWhiteInformation(
    serviceName = "example-spring-boot",
    apiName = "ping-pong",
    apiVersion = "1.0.0",
    operationId = "postPong"
  )
  public ResponseEntity<SuccessResponse> postPong(
    @Valid @RequestBody PingRequest pingRequest
  ) {
    return ResponseEntity.ok(
      SuccessResponse.builder()
        .message(pingRequest.getMessage())
        .success(TRUE)
        .build()
    );
  }

  @GetMapping("/pung/{message}")
  @Operation(
    summary = "Send a pung request",
    description = "Send a pung directly (usually in response to a ping)",
    operationId = "getPung"
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Pung successfully sent",
      content = @Content(
        schema = @Schema(implementation = SuccessResponse.class)
      )
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
  })
  @SnowWhiteInformation(
    serviceName = "example-spring-boot",
    apiName = "ping-pong",
    apiVersion = "1.0.0",
    operationId = "getPung"
  )
  public ResponseEntity<SuccessResponse> getPung(
    @Parameter(
      description = "Message to include with the ping",
      example = "Hello server!"
    ) @PathVariable String message
  ) {
    return ResponseEntity.ok(
      SuccessResponse.builder().message(message).success(TRUE).build()
    );
  }
}
