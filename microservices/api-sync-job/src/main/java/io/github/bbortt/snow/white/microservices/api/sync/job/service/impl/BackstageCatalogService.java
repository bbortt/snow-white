/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.Entity;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.EntityMeta;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class BackstageCatalogService implements ApiCatalogService {

  private static final Integer PAGE_SIZE = 10;

  private final ApiSyncJobProperties.BackstageProperties backstageProperties;
  private final EntityApi backstageEntityApi;
  private final JsonMapper jsonMapper;
  private final OpenApiValidationService openApiValidationService;
  private final AsyncTaskExecutor taskExecutor;

  private final InformationExtractor informationExtractor;
  private final OpenAPIV3Parser openAPIV3Parser;

  private final MinioService minioService;

  public BackstageCatalogService(
    ApiSyncJobProperties.BackstageProperties backstageProperties,
    EntityApi backstageEntityApi,
    JsonMapper jsonMapper,
    OpenApiValidationService openApiValidationService,
    AsyncTaskExecutor taskExecutor,
    @Autowired(required = false) @Nullable MinioService minioService
  ) {
    this(
      backstageProperties,
      backstageEntityApi,
      jsonMapper,
      openApiValidationService,
      taskExecutor,
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
    JsonMapper jsonMapper,
    OpenApiValidationService openApiValidationService,
    AsyncTaskExecutor taskExecutor,
    @Autowired(required = false) @Nullable MinioService minioService,
    InformationExtractor informationExtractor,
    OpenAPIV3Parser openAPIV3Parser
  ) {
    this.backstageProperties = backstageProperties;
    this.backstageEntityApi = backstageEntityApi;
    this.jsonMapper = jsonMapper;
    this.openApiValidationService = openApiValidationService;
    this.taskExecutor = taskExecutor;
    this.minioService = minioService;
    this.informationExtractor = informationExtractor;
    this.openAPIV3Parser = openAPIV3Parser;
  }

  @Override
  public CompletableFuture<Set<ApiInformation>> fetchApiIndex() {
    return supplyAsync(
      () -> {
        var streamEnhancer = getStreamEnhancerBasedOnSpecLocation();

        var pagedEntities = new PagedBackstageEntitySpliterator(
          backstageEntityApi,
          PAGE_SIZE
        );
        var entitiesStream = StreamSupport.stream(pagedEntities, false);

        return streamEnhancer
          .apply(entitiesStream)
          .map(this::extractApiInformation)
          .filter(openAPIParameters ->
            nonNull(openAPIParameters.apiInformation())
          )
          .map(openAPIParameters -> {
            if (!hasText(openAPIParameters.sourceUrl())) {
              return minioService.storeBackstageApiEntity(openAPIParameters);
            }
            return openAPIParameters.apiInformation();
          })
          .collect(toSet());
      },
      taskExecutor
    );
  }

  @Override
  public ApiInformation validateApiInformation(ApiInformation apiInformation) {
    return openApiValidationService.validateApiInformationFromIndex(
      apiInformation,
      backstageProperties.getParsingMode()
    );
  }

  @NonNull
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

  @NonNull
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

  @NonNull
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
          taskExecutor.submit(() ->
            new OpenAPIParameters(
              location,
              openAPIV3Parser.readLocation(
                location,
                emptyList(),
                new ParseOptions()
              )
            )
          )
        )
        .map(future -> {
          try {
            return future.get();
          } catch (Exception e) {
            logger.error("Error resolving from custom repository", e);
            if (e instanceof InterruptedException) {
              currentThread().interrupt();
            }
            return null;
          }
        })
        .filter(Objects::nonNull);
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
          Optional.ofNullable(spec.get("type"))
            .map(String::valueOf)
            .orElse("")
            .equals("openapi")
        )
        .map(jsonMapper::<JsonNode>valueToTree)
        .map(jsonNode -> jsonNode.get("definition"))
        .map(JsonNode::asString)
        .map(definition ->
          taskExecutor.submit(() ->
            new OpenAPIParameters(openAPIV3Parser.readContents(definition))
          )
        )
        .map(future -> {
          try {
            return future.get();
          } catch (Exception e) {
            logger.error("Error resolving from backstage", e);
            if (e instanceof InterruptedException) {
              currentThread().interrupt();
            }
            return null;
          }
        })
        .filter(Objects::nonNull);
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
}
