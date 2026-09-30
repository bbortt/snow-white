/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.DOWNLOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PARSE_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesNf;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
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

  /**
   * Reads the identity triple out of the document itself, at three JSON paths
   * the operator may override - the same extraction the CLI applies to a
   * prerelease upload, so one specification resolves one identity whichever way
   * it enters Snow-White.
   */
  @RealizesArch(ArchTraceables.ARCH_015_IDENTITY_DECLARED_IN_THE_SPECIFICATION)
  private final InformationExtractor informationExtractor;

  private final OpenAPIV3Parser openAPIV3Parser;

  /**
   * Only the literal `info` fields are read from a parsed spec, and a
   * conforming document cannot reach them through a `$ref`, so resolution stays
   * off unless an operator asks for it - resolving reaches out to the
   * filesystem or the network once per pointer, for every candidate file.
   */
  private final ParseOptions parseOptions;

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

    this.parseOptions = new ParseOptions();
    this.parseOptions.setResolve(artifactoryProperties.getResolveReferences());
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
      .map(
        repoPath ->
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

  /**
   * Each stage a candidate file can fail at - reading it, parsing it as
   * OpenAPI, reading an identity out of it - answers with its own status, so
   * the cycle's summary says how many files it could not index and why rather
   * than collapsing every miss into one bucket.
   */
  @RealizesArch(ArchTraceables.ARCH_015_IDENTITY_DECLARED_IN_THE_SPECIFICATION)
  @RealizesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
  private ApiInformation fetchItemAndExtractApiInformation(
    String repository,
    AqlItem repoPath
  ) {
    var basePath = repoPath.getPath();
    var filePath =
      basePath.equals(".") || basePath.isEmpty()
        ? repoPath.getName()
        : basePath + "/" + repoPath.getName();

    logger.debug("Downloading item: {}", filePath);

    String content;
    try {
      content = downloadFile(repository, filePath);
    } catch (Exception e) {
      return skipOrAbort(
        DOWNLOAD_FAILED,
        format("Failed to download '%s'", filePath),
        e
      );
    }

    SwaggerParseResult swaggerParseResult;
    try {
      swaggerParseResult = parseFile(content);
    } catch (Exception | StackOverflowError e) {
      // Resolving `$ref` pointers walks them, so a circular chain exhausts the
      // stack rather than raising. Letting that escape kills the worker and
      // drops the file from the summary it belongs in - counted as unparseable
      // is what it is.
      return skipOrAbort(
        PARSE_FAILED,
        format("Failed to parse OpenAPI from '%s'", filePath),
        e
      );
    }

    var openAPI = swaggerParseResult.getOpenAPI();

    // A document can parse into an `OpenAPI` without an `info` object - the
    // identity fields live there, so it is no more indexable than one that did
    // not parse at all.
    if (isNull(openAPI) || isNull(openAPI.getInfo())) {
      return skipOrAbort(
        PARSE_FAILED,
        format(
          "Failed to parse OpenAPI from '%s': %s",
          filePath,
          swaggerParseResult.getMessages()
        ),
        null
      );
    }

    try {
      return extractApiInformation(repository, filePath, openAPI);
    } catch (Exception e) {
      return skipOrAbort(
        LOAD_FAILED,
        format("Failed to extract API information from '%s'", filePath),
        e
      );
    }
  }

  private ApiInformation extractApiInformation(
    String repository,
    String filePath,
    OpenAPI openAPI
  ) {
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

    logger.debug(
      "API information for validation: {}",
      JsonMapper.shared().writeValueAsString(apiInformation)
    );

    return openApiValidationService.validateApiInformationFromIndex(
      apiInformation,
      artifactoryProperties.getParsingMode()
    );
  }

  /**
   * Graceful is the default: the file is skipped, counted under the reason it
   * failed, and the rest of the cycle still publishes. Strict turns the same
   * failure into a raise, which aborts the cycle.
   *
   * <p>The raise carries the cause's own message, not just the cause: what
   * names the candidate file is this message, and what says why it could not be
   * indexed is the one underneath - naming which of a specification's mandatory
   * fields is missing, for instance. An abort is read from a log line, so both
   * belong in the line rather than one of them a {@code getCause()} away.
   */
  @RealizesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
  private ApiInformation skipOrAbort(
    ApiLoadStatus reason,
    String errorMessage,
    @Nullable Throwable cause
  ) {
    if (STRICT.equals(artifactoryProperties.getParsingMode())) {
      var causeMessage = isNull(cause) ? null : cause.getMessage();

      throw isNull(cause)
        ? new ApiCatalogException(errorMessage)
        : new ApiCatalogException(
            isNull(causeMessage)
              ? errorMessage
              : format("%s: %s", errorMessage, causeMessage),
            cause
          );
    }

    logger.warn(errorMessage, cause);

    return ApiInformation.builder().build().withLoadStatus(reason);
  }

  private String downloadFile(String repository, String filePath)
    throws IOException {
    try (
      var inputStream = artifactory
        .repository(repository)
        .download(filePath)
        .doDownload();
      var reader = new InputStreamReader(inputStream, UTF_8)
    ) {
      var content = new StringWriter();
      reader.transferTo(content);
      return content.toString();
    }
  }

  @RealizesNf(NfTraceables.NF_010_REFERENCE_RESOLUTION_IS_OFF_BY_DEFAULT)
  private SwaggerParseResult parseFile(String content) {
    return openAPIV3Parser.readContents(content, null, parseOptions);
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
