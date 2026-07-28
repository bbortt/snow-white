/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.api;

import static java.lang.Boolean.TRUE;

import io.github.bbortt.snow.white.example.application.model.PingRequest;
import io.github.bbortt.snow.white.example.application.model.PongResponse;
import io.github.bbortt.snow.white.example.application.model.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingPongApiImpl implements PingPongApi {

  @Override
  public ResponseEntity<PongResponse> getPing(String message) {
    return ResponseEntity.ok(PongResponse.builder().message(message).build());
  }

  @Override
  public ResponseEntity<SuccessResponse> postPong(PingRequest pingRequest) {
    return ResponseEntity.ok(
      SuccessResponse.builder()
        .message(pingRequest.getMessage())
        .success(TRUE)
        .build()
    );
  }

  @Override
  public ResponseEntity<SuccessResponse> getPung(String message) {
    return ResponseEntity.ok(
      SuccessResponse.builder().message(message).success(TRUE).build()
    );
  }
}
