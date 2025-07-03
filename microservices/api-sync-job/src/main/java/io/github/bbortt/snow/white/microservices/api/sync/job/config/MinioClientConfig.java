/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static org.springframework.util.StringUtils.hasText;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(PREFIX + ".minio.endpoint")
public class MinioClientConfig {

  @Bean
  public MinioClient minioClient(ApiSyncJobProperties apiSyncJobProperties) {
    var minio = apiSyncJobProperties.getMinio();

    var minioClientBuilder = MinioClient.builder().endpoint(
      minio.getEndpoint()
    );

    if (hasText(minio.getAccessKey()) && hasText(minio.getSecretKey())) {
      minioClientBuilder = minioClientBuilder.credentials(
        minio.getAccessKey(),
        minio.getSecretKey()
      );
    }

    return minioClientBuilder.build();
  }
}
