/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.housekeeping.HousekeepingJob;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HousekeepingService {

  private final List<HousekeepingJob> housekeepingJobs;
  private final Executor virtualThreadExecutor;

  public void runHousekeeping() {
    logger.info("Invoking housekeeping jobs");
    housekeepingJobs.forEach(virtualThreadExecutor::execute);
  }

  @Scheduled(cron = "${snow.white.report.coordinator.api.housekeeping.cron:-}")
  public void runScheduledHousekeeping() {
    logger.info("Invoking scheduled housekeeping jobs");
    runHousekeeping();
  }
}
