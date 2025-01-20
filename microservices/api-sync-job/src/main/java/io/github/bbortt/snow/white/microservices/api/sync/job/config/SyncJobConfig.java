package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.jackson.ApiDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncJobConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(
    ApiSyncJobProperties apiSyncJobProperties
  ) {
    return builder ->
      builder.deserializers(new ApiDeserializer(apiSyncJobProperties));
  }
}
