/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application.api;

import io.github.bbortt.snow.white.example.application.model.PongResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingApiImpl implements PingApi {

  @Override
  public ResponseEntity<PongResponse> getPing(String message) {
    return ResponseEntity.ok(PongResponse.builder().message(message).build());
  }
}
