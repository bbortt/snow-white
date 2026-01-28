/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.String.format;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.model.AqlItem;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.InvalidFileSpecException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class ArtifactoryApiCatalogService implements ApiCatalogService {

  private final Artifactory artifactory;
  private final ApiSyncJobProperties.ArtifactoryProperties artifactoryProperties;
  private final OpenApiValidationService openApiValidationService;

  private final InformationExtractor informationExtractor;
  private final OpenAPIV3Parser openAPIV3Parser;

  @Autowired
  public ArtifactoryApiCatalogService(
    Artifactory artifactory,
    ApiSyncJobProperties apiSyncJobProperties,
    OpenApiValidationService openApiValidationService
  ) {
    this(
      artifactory,
      apiSyncJobProperties,
      openApiValidationService,
      new InformationExtractor(
        apiSyncJobProperties.getArtifactory().getCustomApiNameJsonPath(),
        apiSyncJobProperties.getArtifactory().getCustomApiVersionJsonPath(),
        apiSyncJobProperties.getArtifactory().getCustomServiceNameJsonPath()
      ),
      new OpenAPIV3Parser()
    );
  }

  @VisibleForTesting
  ArtifactoryApiCatalogService(
    Artifactory artifactory,
    ApiSyncJobProperties apiSyncJobProperties,
    OpenApiValidationService openApiValidationService,
    InformationExtractor informationExtractor,
    OpenAPIV3Parser openAPIV3Parser
  ) {
    this.artifactory = artifactory;
    this.artifactoryProperties = apiSyncJobProperties.getArtifactory();
    this.openApiValidationService = openApiValidationService;
    this.informationExtractor = informationExtractor;
    this.openAPIV3Parser = openAPIV3Parser;
  }

  public List<Supplier<@Nullable ApiInformation>> getApiSpecificationLoaders() {
    var repository = artifactoryProperties.getRepository();
    List<AqlItem> repoPaths = artifactory
      .searches()
      .repositories(repository)
      .artifactsByFileSpec(getFileSpecMatchingAllApiSpecifications());

    logger.info(
      "Found {} items in repository: {}",
      repoPaths.size(),
      repository
    );

    return repoPaths
      .parallelStream()
      .map(repoPath ->
        (Supplier<ApiInformation>) () ->
          fetchItemAndExtractApiInformation(repository, repoPath)
      )
      .toList();
  }

  private FileSpec getFileSpecMatchingAllApiSpecifications() {
    try {
      return FileSpec.fromString(
        format(
          // language=json
          """
          {
            "files": [
             {
                "pattern": "%s/*.json"
              },
              {
                "pattern": "%s/*.yml"
              },
              {
                "pattern": "%s/*.yaml"
              }
            ]
          }
          """,
          artifactoryProperties.getRepository(),
          artifactoryProperties.getRepository(),
          artifactoryProperties.getRepository()
        )
      );
    } catch (InvalidFileSpecException e) {
      throw new IllegalArgumentException(
        "Failed to create AQL pattern - this should never happen!",
        e
      );
    }
  }

  private @Nullable ApiInformation fetchItemAndExtractApiInformation(
    String repository,
    AqlItem repoPath
  ) {
    var basePath = repoPath.getPath();
    var filePath =
      basePath.equals(".") || basePath.isEmpty()
        ? repoPath.getName()
        : basePath + "/" + repoPath.getName();

    try (
      var inputStream = artifactory
        .repository(repository)
        .download(filePath)
        .doDownload()
    ) {
      var swaggerParseResult = openAPIV3Parser.readContents(
        new String(inputStream.readAllBytes())
      );
      var openAPI = swaggerParseResult.getOpenAPI();

      if (openAPI == null) {
        var errorMessage = format(
          "Failed to parse OpenAPI from '%s': %s",
          filePath,
          swaggerParseResult.getMessages()
        );

        if (STRICT.equals(artifactoryProperties.getParsingMode())) {
          throw new ApiCatalogException(errorMessage);
        } else {
          logger.warn(errorMessage);
          return null;
        }
      }

      var openApiInformation = informationExtractor.extractFromOpenApi(
        openApiAsJson(openAPI)
      );

      var downloadUrl = fetchPublicDownloadUrl(repository, filePath);

      var apiInformation = ApiInformation.builder()
        .title(openAPI.getInfo().getTitle())
        .version(openApiInformation.apiVersion())
        .sourceUrl(downloadUrl)
        .name(openApiInformation.apiName())
        .serviceName(openApiInformation.serviceName())
        .apiType(OPENAPI)
        .build();

      return openApiValidationService.validateApiInformationFromIndex(
        apiInformation,
        artifactoryProperties.getParsingMode()
      );
    } catch (Exception e) {
      var errorMessage = format("Failed to parse OpenAPI from '%s'", filePath);

      if (STRICT.equals(artifactoryProperties.getParsingMode())) {
        throw new ApiCatalogException(errorMessage, e);
      } else {
        logger.warn(errorMessage, e);
        return null;
      }
    }
  }

  private String openApiAsJson(OpenAPI openAPI) {
    return JsonMapper.shared().writeValueAsString(openAPI);
  }

  private @NonNull String fetchPublicDownloadUrl(
    String repository,
    String filePath
  ) {
    var info = artifactory.repository(repository).file(filePath).info();
    if (!(info instanceof File fileInfo)) {
      throw new IllegalStateException(
        "Encountered OpenAPI specification which is not a file!"
      );
    }

    return fileInfo.getDownloadUri();
  }
}
