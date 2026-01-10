/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(MinioClient.class)
public class MinioService {

  private final MinioClient minioClient;

  private final String bucketName;
  private final String endpoint;

  public MinioService(
    ApiSyncJobProperties apiSyncJobProperties,
    MinioClient minioClient
  ) {
    this.minioClient = minioClient;

    var minIOProperties = apiSyncJobProperties.getMinio();
    bucketName = minIOProperties.getBucketName();
    endpoint = minIOProperties.getEndpoint();
  }

  /**
   * Backstage does not have direct access links to API entities, or I didn't find it.
   * Thus, Snow-White stores them in an S3 bucket.
   */
  ApiInformation storeBackstageApiEntity(OpenAPIParameters openAPIParameters) {
    requireNonNull(openAPIParameters.apiInformation());

    var objectName = constructFileName(openAPIParameters.apiInformation());

    try {
      minioClient.putObject(
        PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .stream(
            new ByteArrayInputStream(
              openAPIParameters.openApiAsJson().getBytes(UTF_8)
            ),
            -1,
            10485760
          ) // 10MB part size
          .build()
      );
    } catch (Exception e) {
      throw new MinioException(e);
    }

    var publicUrl = format(
      "%s/%s/%s",
      sanitizedEndpoint(endpoint),
      bucketName,
      objectName
    );

    return openAPIParameters.apiInformation().withSourceUrl(publicUrl);
  }

  private String constructFileName(ApiInformation openAPIParameters) {
    return format(
      "%s-%s-%s.xml",
      openAPIParameters.getServiceName(),
      openAPIParameters.getName(),
      openAPIParameters.getVersion()
    ).replace(" ", "-");
  }

  private String sanitizedEndpoint(String endpoint) {
    return endpoint.endsWith("/")
      ? endpoint.substring(0, endpoint.length() - 1)
      : endpoint;
  }

  public static class MinioException extends RuntimeException {

    public MinioException(Throwable cause) {
      super("Failed to store object in MinIO!", cause);
    }
  }
}
