/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.PREFIX;
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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

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
  class RunHousekeepingTest {

    static Stream<Consumer<HousekeepingService>> housekeepingMethods() {
      return Stream.of(
        HousekeepingService::runHousekeeping,
        HousekeepingService::runScheduledHousekeeping
      );
    }

    @ParameterizedTest
    @MethodSource("housekeepingMethods")
    void runsAllJobsAsynchronously(
      Consumer<HousekeepingService> housekeepingMethod
    ) {
      fixture = new HousekeepingService(
        List.of(firstHousekeepingJobMock, secondHousekeepingJobMock),
        new SimpleAsyncTaskExecutor()
      );

      housekeepingMethod.accept(fixture);

      // Jobs run on virtual threads — use a generous but bounded timeout
      verify(firstHousekeepingJobMock, timeout(1_000)).run();
      verify(secondHousekeepingJobMock, timeout(1_000)).run();
    }

    @ParameterizedTest
    @MethodSource("housekeepingMethods")
    void returnsImmediatelyWithoutWaitingForJobs(
      Consumer<HousekeepingService> housekeepingMethod
    ) {
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
      housekeepingMethod.accept(fixture);

      long elapsed = currentTimeMillis() - start;
      assertThat(elapsed).isLessThan(1_000);
    }

    @ParameterizedTest
    @MethodSource("housekeepingMethods")
    void withNoJobs_returns(Consumer<HousekeepingService> housekeepingMethod) {
      fixture = new HousekeepingService(
        emptyList(),
        new SimpleAsyncTaskExecutor()
      );

      assertThatCode(() ->
        housekeepingMethod.accept(fixture)
      ).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("housekeepingMethods")
    void continuesRunningRemainingJobsWhenOneThrows(
      Consumer<HousekeepingService> housekeepingMethod
    ) {
      HousekeepingJob failingJob = () -> {
        throw new RuntimeException("boom");
      };
      fixture = new HousekeepingService(
        List.of(failingJob, secondHousekeepingJobMock),
        new SimpleAsyncTaskExecutor()
      );

      housekeepingMethod.accept(fixture);

      verify(secondHousekeepingJobMock, timeout(1_000)).run();
    }
  }

  @Nested
  class RunScheduledHousekeepingTest {

    @Test
    void scheduledAnnotationShouldStartWithPrefix()
      throws NoSuchMethodException {
      assertThat(
        HousekeepingService.class.getDeclaredMethod("runScheduledHousekeeping")
          .getDeclaredAnnotation(Scheduled.class)
          .cron()
      ).startsWith("${" + PREFIX + ".housekeeping");
    }
  }
}
