/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
class ApiErrorAttributes extends DefaultErrorAttributes {

  @Override
  public Map<String, Object> getErrorAttributes(
    WebRequest request,
    ErrorAttributeOptions options
  ) {
    var attrs = super.getErrorAttributes(request, options);
    return Map.of(
      "code",
      String.valueOf(attrs.getOrDefault("error", "Internal Server Error")),
      "message",
      String.valueOf(
        attrs.getOrDefault("message", "An unexpected error occurred")
      )
    );
  }
}
