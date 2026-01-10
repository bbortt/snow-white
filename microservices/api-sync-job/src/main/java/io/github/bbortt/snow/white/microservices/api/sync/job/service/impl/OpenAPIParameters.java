package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.jspecify.annotations.Nullable;

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

  public OpenAPIParameters(String contents) {
    this(null, new OpenAPIV3Parser().readContents(contents), null);
  }

  public OpenAPIParameters(
    String sourceUrl,
    SwaggerParseResult swaggerParseResult,
    ApiInformation apiInformation
  ) {
    this.sourceUrl = sourceUrl;
    this.swaggerParseResult = swaggerParseResult;
    this.apiInformation = apiInformation;
  }

  public OpenAPIParameters withApiInformation(ApiInformation apiInformation) {
    return new OpenAPIParameters(sourceUrl, swaggerParseResult, apiInformation);
  }

  public String openApiAsJson() {
    return swaggerParseResult.getOpenAPI().toString();
  }
}
