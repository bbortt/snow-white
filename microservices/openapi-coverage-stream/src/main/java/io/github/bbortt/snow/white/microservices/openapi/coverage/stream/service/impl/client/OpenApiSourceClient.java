/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import static java.util.Collections.emptyList;

import io.swagger.v3.parser.util.RemoteUrl;
import java.io.IOException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class OpenApiSourceClient {

  @Retryable(
    retryFor = { IOException.class },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public String fetchContent(String sourceUrl) throws IOException {
    try {
      return RemoteUrl.urlToString(sourceUrl, emptyList());
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }
}
