/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvalidReleaseWithContentExceptionTest {

  @Test
  void constructsMessage() {
    assertThat(new InvalidReleaseWithContentException()).hasMessage(
      "Attempted to persist API information that is not a prerelease with content: Snow-White is not an service index repository!"
    );
  }
}
