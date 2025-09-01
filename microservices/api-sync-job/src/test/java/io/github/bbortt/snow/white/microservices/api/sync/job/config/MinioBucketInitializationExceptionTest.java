/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinioBucketInitializationExceptionTest {

  @Test
  void constructorAssignsMessageAndCause() {
    var bucketName = "bucketName";
    var cause = new Throwable("Something nasty happened!");

    var fixture = new MinioBucketInitializationException(bucketName, cause);
    assertThat(fixture).satisfies(
      f ->
        assertThat(f).hasMessage(
          "Failed to initialize MinIO bucket 'bucketName'!"
        ),
      f -> assertThat(f).hasCause(cause)
    );
  }
}
