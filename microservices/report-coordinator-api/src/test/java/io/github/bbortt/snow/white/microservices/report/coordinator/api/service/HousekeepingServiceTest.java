/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.housekeeping.HousekeepingJob;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@ExtendWith({ MockitoExtension.class })
class HousekeepingServiceTest {

  @Mock
  private HousekeepingJob firstHousekeepingJobMock;

  @Mock
  private HousekeepingJob secondHousekeepingJobMock;

  private HousekeepingService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new HousekeepingService(
      List.of(firstHousekeepingJobMock, secondHousekeepingJobMock),
      new SimpleAsyncTaskExecutor()
    );
  }

  @Nested
  class HousekeepingTest {

    @Test
    void runsAllJobsAsynchronously() {
      fixture = new HousekeepingService(
        List.of(firstHousekeepingJobMock, secondHousekeepingJobMock),
        new SimpleAsyncTaskExecutor()
      );

      fixture.runHousekeeping();

      // Jobs run on virtual threads — use a generous but bounded timeout
      verify(firstHousekeepingJobMock, timeout(1_000)).run();
      verify(secondHousekeepingJobMock, timeout(1_000)).run();
    }

    @Test
    void returnsImmediatelyWithoutWaitingForJobs() {
      HousekeepingJob slowJob = () -> {
        try {
          sleep(5_000);
        } catch (InterruptedException _) {
          currentThread().interrupt();
        }
      };

      fixture = new HousekeepingService(
        singletonList(slowJob),
        new SimpleAsyncTaskExecutor()
      );

      long start = currentTimeMillis();
      fixture.runHousekeeping();

      long elapsed = currentTimeMillis() - start;
      assertThat(elapsed).isLessThan(1_000);
    }

    @Test
    void withNoJobs_returns() {
      fixture = new HousekeepingService(
        emptyList(),
        new SimpleAsyncTaskExecutor()
      );

      assertThatCode(fixture::runHousekeeping).doesNotThrowAnyException();
    }

    @Test
    void continuesRunningRemainingJobsWhenOneThrows() {
      HousekeepingJob failingJob = () -> {
        throw new RuntimeException("boom");
      };
      fixture = new HousekeepingService(
        List.of(failingJob, secondHousekeepingJobMock),
        new SimpleAsyncTaskExecutor()
      );

      fixture.runHousekeeping();

      verify(secondHousekeepingJobMock, timeout(1_000)).run();
    }
  }
}
