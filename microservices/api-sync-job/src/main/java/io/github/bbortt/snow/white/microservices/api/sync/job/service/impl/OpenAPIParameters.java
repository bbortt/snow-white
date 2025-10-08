/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

public record OpenAPIParameters(
  @Nullable String sourceUrl,
  SwaggerParseResult swaggerParseResult,
  @Nullable ApiInformation apiInformation
) {
  public OpenAPIParameters(SwaggerParseResult swaggerParseResult) {
    this(null, swaggerParseResult, null);
  }

  public OpenAPIParameters(
    String sourceUrl,
    SwaggerParseResult swaggerParseResult
  ) {
    this(sourceUrl, swaggerParseResult, null);
  }

  public OpenAPIParameters withApiInformation(ApiInformation apiInformation) {
    return new OpenAPIParameters(sourceUrl, swaggerParseResult, apiInformation);
  }

  public String openApiAsJson() {
    var openAPI = swaggerParseResult().getOpenAPI();
    return JsonMapper.shared().writeValueAsString(openAPI);
  }
}
