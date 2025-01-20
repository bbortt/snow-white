package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

@IntegrationTest
class SyncJobConfigIT {

  @Autowired
  private ApiSyncJobProperties apiSyncJobProperties;

  @Autowired
  private SyncJobConfig syncJobConfig;

  @Autowired
  private Jackson2ObjectMapperBuilderCustomizer jsonCustomizer;

  @Test
  void jsonCustomizerIsBean() {
    assertThat(jsonCustomizer).isEqualTo(
      syncJobConfig.jsonCustomizer(apiSyncJobProperties)
    );
  }
}
