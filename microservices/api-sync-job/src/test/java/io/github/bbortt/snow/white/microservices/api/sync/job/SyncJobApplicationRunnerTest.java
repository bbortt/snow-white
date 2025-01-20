package io.github.bbortt.snow.white.microservices.api.sync.job;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

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
  void runInvokesSyncJob() {
    fixture.run(new DefaultApplicationArguments());

    verify(syncJobMock).queryAndSafeApiCatalogArtifacts();
  }
}
