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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(
  value = PREFIX + ".minio.init-bucket",
  havingValue = "true"
)
public class MinioBucketConfig {

  @VisibleForTesting
  static final String PUBLIC_BUCKET_POLICY_TEMPLATE =
    """
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

  public MinioBucketConfig(
    ApiSyncJobProperties apiSyncJobProperties,
    MinioClient minioClient
  )
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    var bucketName = apiSyncJobProperties.getMinio().getBucketName();

    logger.info("Initializing bucket '{}'", bucketName);

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
