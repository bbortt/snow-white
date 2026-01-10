/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.annotation.Nullable;
import lombok.With;

@With
public record OpenAPIParameters(
  @Nullable String sourceUrl,
  SwaggerParseResult swaggerParseResult,
  @Nullable ApiInformation apiInformation
) {
  public OpenAPIParameters(SwaggerParseResult swaggerParseResult) {
    this(null, swaggerParseResult, null);
  }

  public OpenAPIParameters(
    String location,
    SwaggerParseResult swaggerParseResult
  ) {
    this(location, swaggerParseResult, null);
  }

  public String openApiAsJson() {
    var openAPI = swaggerParseResult().getOpenAPI();

    try {
      return Json.mapper().writeValueAsString(openAPI);
    } catch (JsonProcessingException e) {
      throw new OpenApiProcessingException(e);
    }
  }
}
