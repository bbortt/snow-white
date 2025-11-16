/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

class TaskExecutorConfigTest {

  private TaskExecutorConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new TaskExecutorConfig();
  }

  @Nested
  class ApplicationTaskExecutorTest {

    @Test
    void returnsNewTaskExecutor() {
      var result = fixture.applicationTaskExecutor();
      assertThat(result).isInstanceOf(AsyncTaskExecutor.class);
    }
  }
}
