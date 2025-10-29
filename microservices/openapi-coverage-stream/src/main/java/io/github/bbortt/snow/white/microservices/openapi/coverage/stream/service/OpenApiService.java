/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenApiService {

  private static final OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

  private final ApiEndpointRepository apiEndpointRepository;

  public OpenAPI findAndParseOpenApi(ApiInformation apiInformation)
    throws OpenApiNotIndexedException, UnparseableOpenApiException {
    var apiEndpointEntry = apiEndpointRepository
      .findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
        apiInformation.getServiceName(),
        apiInformation.getApiName(),
        apiInformation.getApiVersion()
      )
      .orElseThrow(() -> new OpenApiNotIndexedException(apiInformation));

    return parseOpenApiSource(apiEndpointEntry);
  }

  private OpenAPI parseOpenApiSource(@NonNull ApiEndpointEntry apiEndpointEntry)
    throws UnparseableOpenApiException {
    SwaggerParseResult swaggerParseResult = openAPIV3Parser.readLocation(
      apiEndpointEntry.getSourceUrl(),
      emptyList(),
      new ParseOptions()
    );

    if (!isEmpty(swaggerParseResult.getMessages())) {
      throw new UnparseableOpenApiException(swaggerParseResult.getMessages());
    }

    return swaggerParseResult.getOpenAPI();
  }
}
