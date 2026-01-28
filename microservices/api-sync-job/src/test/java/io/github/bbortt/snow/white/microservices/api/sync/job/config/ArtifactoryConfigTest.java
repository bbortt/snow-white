/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ArtifactoryConfigTest {

  private static final String BASE_URL = "http://jfrog.artifactory";
  private static final String ACCESS_TOKEN = "secret-access-token";

  private ApiSyncJobProperties apiSyncJobProperties;

  private ArtifactoryConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    apiSyncJobProperties = new ApiSyncJobProperties();

    fixture = new ArtifactoryConfig(apiSyncJobProperties);
  }

  @Nested
  class ArtifactoryTest {

    @Test
    void shouldExtractConfigurationFromProperties() {
      apiSyncJobProperties.getArtifactory().setBaseUrl(BASE_URL);
      apiSyncJobProperties.getArtifactory().setAccessToken(ACCESS_TOKEN);

      var artifactory = fixture.artifactory();

      assertThat(artifactory)
        .hasFieldOrPropertyWithValue("url", BASE_URL)
        .hasFieldOrPropertyWithValue("accessToken", ACCESS_TOKEN);
    }
  }
}
