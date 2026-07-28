/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static org.springframework.http.HttpStatus.ACCEPTED;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.HousekeepingApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.HousekeepingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HousekeepingResource implements HousekeepingApi {

  private final HousekeepingService houseKeepingService;

  @Override
  public ResponseEntity<Void> housekeeping() {
    houseKeepingService.runHousekeeping();
    return ResponseEntity.status(ACCEPTED).build();
  }
}
