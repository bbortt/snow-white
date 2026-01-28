/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArtifactoryConfig {

  private final ApiSyncJobProperties.ArtifactoryProperties artifactoryProperties;

  public ArtifactoryConfig(ApiSyncJobProperties apiSyncJobProperties) {
    this.artifactoryProperties = apiSyncJobProperties.getArtifactory();
  }

  @Bean
  public Artifactory artifactory() {
    return ArtifactoryClientBuilder.create()
      .setUrl(artifactoryProperties.getBaseUrl())
      .setAccessToken(artifactoryProperties.getAccessToken())
      .build();
  }
}
