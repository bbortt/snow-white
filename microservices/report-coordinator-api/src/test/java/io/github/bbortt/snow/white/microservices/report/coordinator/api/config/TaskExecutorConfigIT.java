/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;

class TaskExecutorConfigIT extends AbstractReportCoordinationServiceIT {

  @Autowired
  private AsyncTaskExecutor applicationTaskExecutor;

  @Autowired
  private Clock clock;

  @Test
  void applicationContext_containsAsyncTaskExecutorBean() {
    assertThat(applicationTaskExecutor).isNotNull();
  }

  @Test
  void applicationContext_containsClockBean() {
    assertThat(clock).isNotNull();
  }
}
