/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class SyncJobApplicationRunnerUnitTest {

  @Mock
  private SyncJob syncJobMock;

  @InjectMocks
  private SyncJobApplicationRunner fixture;

  @Test
  void shouldBePresent_whenProfileIsNotTest() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      SyncJobApplicationRunner.class
    );

    contextRunner
      .withBean(SyncJob.class, () -> syncJobMock)
      .run(context ->
        assertThat(context).hasSingleBean(SyncJobApplicationRunner.class)
      );
  }

  @Test
  void shouldBeDisabled_whenInTestProfile() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      SyncJobApplicationRunner.class
    );

    contextRunner
      .withBean(SyncJob.class, () -> syncJobMock)
      .withPropertyValues("spring.profiles.active=test")
      .run(context ->
        assertThat(context).doesNotHaveBean(SyncJobApplicationRunner.class)
      );

    verifyNoInteractions(syncJobMock);
  }

  @Nested
  class RunTest {

    @Test
    @VerifiesArch(ArchTraceables.ARCH_014_SYNC_CADENCE_OWNED_BY_THE_SCHEDULER)
    void shouldInvokeSyncCatalogExactlyOncePerRun()
      throws InterruptedException {
      fixture.run(new DefaultApplicationArguments());

      verify(syncJobMock, times(1)).syncCatalog();
      verifyNoMoreInteractions(syncJobMock);
    }
  }
}
