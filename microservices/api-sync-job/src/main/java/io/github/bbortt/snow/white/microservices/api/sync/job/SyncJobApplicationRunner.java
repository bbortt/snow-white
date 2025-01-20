package io.github.bbortt.snow.white.microservices.api.sync.job;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SyncJobApplicationRunner implements ApplicationRunner {

  private final SyncJob syncJob;

  public SyncJobApplicationRunner(SyncJob syncJob) {
    this.syncJob = syncJob;
  }

  @Override
  public void run(ApplicationArguments args) {
    syncJob.queryAndSafeApiCatalogArtifacts();
  }
}
