/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.Entity;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.EntityMeta;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BackstageCatalogService implements ApiCatalogService {

  private static final Integer PAGE_SIZE = 10;

  private final ApiSyncJobProperties.BackstageProperties backstageProperties;
  private final EntityApi backstageEntityApi;
  private final ObjectMapper objectMapper;
  private final OpenApiValidationService openApiValidationService;

  private final InformationExtractor informationExtractor;
  private final OpenAPIV3Parser openAPIV3Parser;

  private MinioService minioService;

  public BackstageCatalogService(
    ApiSyncJobProperties.BackstageProperties backstageProperties,
    EntityApi backstageEntityApi,
    ObjectMapper objectMapper,
    OpenApiValidationService openApiValidationService,
    @Autowired(required = false) @Nullable MinioService minioService
  ) {
    this(
      backstageProperties,
      backstageEntityApi,
      objectMapper,
      openApiValidationService,
      minioService,
      new InformationExtractor(
        backstageProperties.getCustomApiNameJsonPath(),
        backstageProperties.getCustomApiVersionJsonPath(),
        backstageProperties.getCustomServiceNameJsonPath()
      ),
      new OpenAPIV3Parser()
    );
  }

  BackstageCatalogService(
    ApiSyncJobProperties.BackstageProperties backstageProperties,
    EntityApi backstageEntityApi,
    ObjectMapper objectMapper,
    OpenApiValidationService openApiValidationService,
    @Autowired(required = false) @Nullable MinioService minioService,
    InformationExtractor informationExtractor,
    OpenAPIV3Parser openAPIV3Parser
  ) {
    this.backstageProperties = backstageProperties;
    this.backstageEntityApi = backstageEntityApi;
    this.objectMapper = objectMapper;
    this.openApiValidationService = openApiValidationService;

    this.informationExtractor = informationExtractor;
    this.openAPIV3Parser = openAPIV3Parser;

    this.minioService = minioService;
  }

  @Override
  public Set<ApiInformation> fetchApiIndex() {
    var streamEnhancer = getStreamEnhancerBasedOnSpecLocation();
    var totalItems = queryTotalApiEntities();

    var apiInformation = new HashSet<ApiInformation>();

    for (int offset = 0; offset < totalItems.intValue(); offset += PAGE_SIZE) {
      queryAndParsePageIntoApiInformation(streamEnhancer, offset).forEach(
        apiInformation::add
      );
    }

    return apiInformation;
  }

  @Override
  public ApiInformation validateApiInformation(ApiInformation apiInformation) {
    return openApiValidationService.validateApiInformationFromIndex(
      apiInformation,
      backstageProperties.getParsingMode()
    );
  }

  @NotNull
  private Function<
    Stream<Entity>,
    Stream<OpenAPIParameters>
  > getStreamEnhancerBasedOnSpecLocation() {
    if (hasText(backstageProperties.getCustomVersionAnnotation())) {
      logger.debug(
        "Noticed custom OpenAPI repository management - resolving from annotation: {}",
        backstageProperties.getCustomVersionAnnotation()
      );

      return resolveFromCustomRepository();
    } else {
      logger.debug("Resolving OpenAPI's from Backstage");

      if (isNull(minioService)) {
        throw new IllegalArgumentException(
          "MinIO connection is required when resolving Entities from Backstage!"
        );
      }

      return resolveFromBackstage();
    }
  }

  @NotNull
  private BigDecimal queryTotalApiEntities() {
    return backstageEntityApi
      .getEntitiesByQuery(null, 0, null, null, null, null, null, null)
      .getTotalItems();
  }

  @NotNull
  private List<Entity> queryPageItems(int offset) {
    return backstageEntityApi
      .getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        offset,
        null,
        null,
        null,
        null,
        null
      )
      .getItems();
  }

  @NotNull
  private Stream<ApiInformation> queryAndParsePageIntoApiInformation(
    Function<Stream<Entity>, Stream<OpenAPIParameters>> streamEnhancer,
    int offset
  ) {
    return streamEnhancer
      .apply(queryPageItems(offset).parallelStream())
      .map(this::extractApiInformation)
      .filter(openAPIParameters -> nonNull(openAPIParameters.apiInformation()))
      .map(openAPIParameters -> {
        if (!hasText(openAPIParameters.sourceUrl())) {
          return minioService.storeBackstageApiEntity(openAPIParameters);
        }

        return openAPIParameters.apiInformation();
      });
  }

  // TODO: How to make sure this is an OpenAPI?
  private Function<
    Stream<Entity>,
    Stream<OpenAPIParameters>
  > resolveFromCustomRepository() {
    return inStream ->
      inStream
        .map(Entity::getMetadata)
        .map(EntityMeta::getAnnotations)
        .filter(Objects::nonNull)
        .map(annotations ->
          annotations.get(backstageProperties.getCustomVersionAnnotation())
        )
        .filter(Objects::nonNull)
        .map(customVersionAnnotation ->
          stream(customVersionAnnotation.split("\n")).toList()
        )
        .flatMap(Collection::stream)
        .map(location ->
          new OpenAPIParameters(
            location,
            openAPIV3Parser.readLocation(
              location,
              emptyList(),
              new ParseOptions()
            )
          )
        );
  }

  private Function<
    Stream<Entity>,
    Stream<OpenAPIParameters>
  > resolveFromBackstage() {
    return inStream ->
      inStream
        .map(Entity::getSpec)
        .filter(Objects::nonNull)
        // TODO: This currently only supports OpenAPI
        .filter(spec ->
          Optional.ofNullable(spec.get("type")).orElse("").equals("openapi")
        )
        .map(objectMapper::<JsonNode>valueToTree)
        .map(jsonNode -> jsonNode.get("definition"))
        .map(JsonNode::asText)
        .map(openAPIV3Parser::readContents)
        .map(OpenAPIParameters::new);
  }

  private OpenAPIParameters extractApiInformation(
    OpenAPIParameters openAPIParameters
  ) {
    if (!isEmpty(openAPIParameters.swaggerParseResult().getMessages())) {
      if (nonNull(openAPIParameters.sourceUrl())) {
        logger.warn(
          "Failed to parse OpenAPI sourceUrl '{}': {}",
          openAPIParameters.sourceUrl(),
          openAPIParameters.swaggerParseResult().getMessages()
        );
      } else {
        logger.warn(
          "Failed to parse OpenAPI from Backstage: {}",
          openAPIParameters.swaggerParseResult().getMessages()
        );
      }

      return openAPIParameters;
    }

    var openAPI = openAPIParameters.swaggerParseResult().getOpenAPI();
    var openApiInformation = informationExtractor.extractFromOpenApi(
      openAPIParameters.openApiAsJson()
    );

    var apiInformation = ApiInformation.builder()
      .title(openAPI.getInfo().getTitle())
      .version(openApiInformation.apiVersion())
      .sourceUrl(openAPIParameters.sourceUrl())
      .name(openApiInformation.apiName())
      .serviceName(openApiInformation.serviceName())
      .apiType(OPENAPI)
      .build();

    return openAPIParameters.withApiInformation(apiInformation);
  }

  @With
  record OpenAPIParameters(
    @Nullable String sourceUrl,
    SwaggerParseResult swaggerParseResult,
    @Nullable ApiInformation apiInformation
  ) {
    OpenAPIParameters(SwaggerParseResult swaggerParseResult) {
      this(null, swaggerParseResult, null);
    }

    OpenAPIParameters(String location, SwaggerParseResult swaggerParseResult) {
      this(location, swaggerParseResult, null);
    }

    String openApiAsJson() {
      var openAPI = swaggerParseResult().getOpenAPI();

      try {
        return Json.mapper().writeValueAsString(openAPI);
      } catch (JsonProcessingException e) {
        throw new OpenApiProcessingException(e);
      }
    }
  }
}
