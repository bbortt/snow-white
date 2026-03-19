/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

class TaskExecutorConfigTest {

  private TaskExecutorConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new TaskExecutorConfig();
  }

  @Nested
  class ApplicationTaskExecutorTest {

    @Test
    void returnsNonNullBean() {
      AsyncTaskExecutor executor = fixture.virtualThreadExecutor();

      assertThat(executor).isNotNull();
    }

    @Test
    void returnsTaskExecutorAdapter() {
      AsyncTaskExecutor executor = fixture.virtualThreadExecutor();

      assertThat(executor).isInstanceOf(TaskExecutorAdapter.class);
    }

    @Test
    void canSubmitAndRunTask() throws InterruptedException {
      AsyncTaskExecutor executor = fixture.virtualThreadExecutor();
      var wasRun = new CountDownLatch(1);

      executor.submit(wasRun::countDown);

      assertThat(wasRun.await(1, SECONDS)).isTrue();
    }
  }
}
