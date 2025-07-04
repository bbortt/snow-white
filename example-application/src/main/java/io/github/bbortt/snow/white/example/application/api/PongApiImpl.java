/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.api;

import io.github.bbortt.snow.white.example.application.model.PingRequest;
import io.github.bbortt.snow.white.example.application.model.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PongApiImpl implements PongApi {

  @Override
  public ResponseEntity<SuccessResponse> postPong(PingRequest pingRequest) {
    return ResponseEntity.ok(
      SuccessResponse.builder().message(pingRequest.getMessage()).build()
    );
  }
}
