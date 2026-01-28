/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class SyncJobApplicationRunnerTest {

  @Mock
  private SyncJob syncJobMock;

  private SyncJobApplicationRunner fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SyncJobApplicationRunner(syncJobMock);
  }

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
  class Run {

    @Test
    void shouldInvokeSyncCatalog() throws InterruptedException {
      fixture.run(new DefaultApplicationArguments());

      verify(syncJobMock).syncCatalog();
    }
  }
}
