/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.OpenApiSourceClient;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenApiService {

  private static final OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

  private final CachingService cachingService;
  private final OpenApiSourceClient openApiSourceClient;

  @WithSpan
  public OpenAPI findAndParseOpenApi(ApiInformation apiInformation)
    throws OpenApiNotIndexedException, UnparseableOpenApiException {
    var sourceUrl = cachingService.fetchApiSourceUrl(apiInformation);

    return parseOpenApiSource(sourceUrl);
  }

  private OpenAPI parseOpenApiSource(@NonNull String sourceUrl)
    throws UnparseableOpenApiException {
    SwaggerParseResult swaggerParseResult = isRemoteLocation(sourceUrl)
      ? readRemoteOpenApiSource(sourceUrl)
      : openAPIV3Parser.readLocation(
          sourceUrl,
          emptyList(),
          new ParseOptions()
        );

    if (!isEmpty(swaggerParseResult.getMessages())) {
      throw new UnparseableOpenApiException(swaggerParseResult.getMessages());
    }

    return swaggerParseResult.getOpenAPI();
  }

  /**
   * The OpenAPI parser library fetches http(s) locations itself, bypassing our retry setup.
   * Fetching it ourselves through {@link OpenApiSourceClient} instead — which the parser library
   * would otherwise do internally, unretried — lets a transient network failure be retried like
   * every other outgoing call, before handing the already-fetched content to the parser.
   */
  private SwaggerParseResult readRemoteOpenApiSource(String sourceUrl)
    throws UnparseableOpenApiException {
    try {
      var content = openApiSourceClient.fetchContent(sourceUrl);
      return openAPIV3Parser.readContents(
        content,
        emptyList(),
        new ParseOptions(),
        sourceUrl
      );
    } catch (IOException e) {
      throw new UnparseableOpenApiException(
        singletonList(
          "Unable to read location `" +
            sourceUrl +
            "`" +
            (e.getMessage() == null ? "" : ": " + e.getMessage())
        )
      );
    }
  }

  private static boolean isRemoteLocation(String sourceUrl) {
    return sourceUrl.toLowerCase(ROOT).startsWith("http");
  }
}
