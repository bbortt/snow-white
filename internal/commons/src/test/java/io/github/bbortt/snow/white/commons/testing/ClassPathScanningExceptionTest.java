/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClassPathScanningExceptionTest {

  @Test
  void constructorAssignsMessageAndCause() {
    var cause = new ClassNotFoundException("Something nasty happened!");

    var fixture = new ClassPathScanningException(cause);

    assertThat(fixture).hasCause(cause);
  }
}
