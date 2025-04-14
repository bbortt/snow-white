/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson.ApiInformationDeserializer;
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
      builder.deserializers(
        new ApiInformationDeserializer(apiSyncJobProperties)
      );
  }
}
