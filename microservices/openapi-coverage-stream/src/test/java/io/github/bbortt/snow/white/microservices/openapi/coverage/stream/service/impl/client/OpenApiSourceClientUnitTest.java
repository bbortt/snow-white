/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import io.swagger.v3.parser.util.RemoteUrl;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiSourceClientUnitTest {

  private OpenApiSourceClient fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiSourceClient();
  }

  @Nested
  class FetchContentTest {

    @Test
    void shouldReturnContent_whenRemoteUrlSucceeds() throws IOException {
      var sourceUrl = "https://example.com/openapi.yaml";

      try (
        MockedStatic<RemoteUrl> remoteUrlMock = mockStatic(RemoteUrl.class)
      ) {
        remoteUrlMock
          .when(() -> RemoteUrl.urlToString(sourceUrl, emptyList()))
          .thenReturn("openapi: 3.0.0");

        var content = fixture.fetchContent(sourceUrl);

        assertThat(content).isEqualTo("openapi: 3.0.0");
      }
    }

    @Test
    void shouldPropagateIOException_asIs() {
      var sourceUrl = "https://example.com/openapi.yaml";
      var ioException = new IOException("connection refused");

      try (
        MockedStatic<RemoteUrl> remoteUrlMock = mockStatic(RemoteUrl.class)
      ) {
        remoteUrlMock
          .when(() -> RemoteUrl.urlToString(sourceUrl, emptyList()))
          .thenThrow(ioException);

        assertThatThrownBy(() -> fixture.fetchContent(sourceUrl)).isSameAs(
          ioException
        );
      }
    }

    @Test
    void shouldWrapNonIOException_asIOException() {
      var sourceUrl = "https://example.com/openapi.yaml";
      var cause = new IllegalStateException("unexpected parser failure");

      try (
        MockedStatic<RemoteUrl> remoteUrlMock = mockStatic(RemoteUrl.class)
      ) {
        remoteUrlMock
          .when(() -> RemoteUrl.urlToString(sourceUrl, emptyList()))
          .thenThrow(cause);

        assertThatThrownBy(() -> fixture.fetchContent(sourceUrl))
          .isInstanceOf(IOException.class)
          .hasMessage(cause.getMessage())
          .hasCause(cause);
      }
    }
  }
}
