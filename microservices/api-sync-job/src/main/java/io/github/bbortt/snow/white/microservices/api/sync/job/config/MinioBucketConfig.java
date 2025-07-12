/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@ConditionalOnProperty(
  value = PREFIX + ".minio.init-bucket",
  havingValue = "true"
)
public class MinioBucketConfig
  implements ApplicationListener<ApplicationReadyEvent> {

  @VisibleForTesting
  static final String PUBLIC_BUCKET_POLICY_TEMPLATE = """
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": "*",
                "Action": "s3:GetObject",
                "Resource": "arn:aws:s3:::%s/*"
            }
        ]
    }
    """;

  private final MinioClient minioClient;

  private final String bucketName;

  public MinioBucketConfig(
    ApiSyncJobProperties apiSyncJobProperties,
    MinioClient minioClient
  ) {
    this.minioClient = minioClient;
    bucketName = apiSyncJobProperties.getMinio().getBucketName();
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    logger.info("Initializing bucket '{}'", bucketName);

    try {
      initializeMinioBucketIfNotExists();
    } catch (
      ErrorResponseException
      | InsufficientDataException
      | InternalException
      | InvalidKeyException
      | InvalidResponseException
      | IOException
      | NoSuchAlgorithmException
      | ServerException
      | XmlParserException e
    ) {
      throw new MinioBucketInitializationException(bucketName, e);
    }
  }

  private void initializeMinioBucketIfNotExists()
    throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
    if (
      !minioClient.bucketExists(
        BucketExistsArgs.builder().bucket(bucketName).build()
      )
    ) {
      minioClient.makeBucket(
        MakeBucketArgs.builder().bucket(bucketName).build()
      );
    } else {
      logger.debug("Bucket '{}' already exists.", bucketName);
    }

    String policy = PUBLIC_BUCKET_POLICY_TEMPLATE.formatted(bucketName);

    minioClient.setBucketPolicy(
      SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build()
    );
  }
}
