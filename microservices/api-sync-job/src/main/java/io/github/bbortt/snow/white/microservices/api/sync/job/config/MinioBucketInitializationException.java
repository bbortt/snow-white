/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static java.lang.String.format;

public class MinioBucketInitializationException extends RuntimeException {

  public MinioBucketInitializationException(
    String bucketName,
    Throwable cause
  ) {
    super(format("Failed to initialize MinIO bucket '%s'!", bucketName), cause);
  }
}
