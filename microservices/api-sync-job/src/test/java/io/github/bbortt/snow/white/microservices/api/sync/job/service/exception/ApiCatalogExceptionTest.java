/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

class ApiCatalogExceptionTest {

  @Test
  void isInstanceOfJacksonException() {
    assertThat(new ApiCatalogException("test")).isInstanceOf(
      JacksonException.class
    );
  }

  @Test
  void hasMessage() {
    var message = "test";
    assertThat(new ApiCatalogException(message)).hasMessage(message);
  }
}
