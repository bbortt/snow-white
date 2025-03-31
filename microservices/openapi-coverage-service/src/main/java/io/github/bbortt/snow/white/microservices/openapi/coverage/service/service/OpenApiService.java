package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenApiService {

  private static final OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

  private final ApiEndpointRepository apiEndpointRepository;

  public OpenAPI findAndParseOpenApi(OpenApiIdentifier openApiIdentifier)
    throws OpenApiNotIndexedException, UnparseableOpenApiException {
    var apiEndpointEntry = apiEndpointRepository
      .findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
        openApiIdentifier.otelServiceName(),
        openApiIdentifier.apiName(),
        openApiIdentifier.apiVersion()
      )
      .orElseThrow(() -> new OpenApiNotIndexedException(openApiIdentifier));

    return parseOpenApiSource(apiEndpointEntry);
  }

  private OpenAPI parseOpenApiSource(
    @NotNull ApiEndpointEntry apiEndpointEntry
  ) throws UnparseableOpenApiException {
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

  public record OpenApiIdentifier(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {}
}
