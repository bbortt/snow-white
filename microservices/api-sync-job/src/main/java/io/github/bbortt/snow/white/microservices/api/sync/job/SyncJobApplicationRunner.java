/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The job's only entry point: one sync pass per process start, then the context
 * closes and the process exits. Cadence is whatever schedules the process - the
 * shipped chart's {@code CronJob} - and is deliberately not held here.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@RealizesArch(ArchTraceables.ARCH_014_SYNC_CADENCE_OWNED_BY_THE_SCHEDULER)
public class SyncJobApplicationRunner implements ApplicationRunner {

  private final SyncJob syncJob;

  @Override
  public void run(ApplicationArguments args) throws InterruptedException {
    syncJob.syncCatalog();
  }
}
