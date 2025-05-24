/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.util.ObjectUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import java.util.Set;
import org.springframework.web.client.RestClient;

public class ServiceInterfaceCatalogService implements ApiCatalogService {

  private final RestClient serviceInterfaceRepositoryClient;
  private final OpenApiValidationService openApiValidationService;

  private final String serviceInterfaceIndexUri;
  private final ParsingMode parsingMode;

  public ServiceInterfaceCatalogService(
    RestClient.Builder restClientBuilder,
    OpenApiValidationService openApiValidationService,
    ApiSyncJobProperties.ServiceInterfaceProperties serviceInterfaceProperties
  ) {
    this.serviceInterfaceRepositoryClient = restClientBuilder
      .baseUrl(serviceInterfaceProperties.getBaseUrl())
      .build();
    this.openApiValidationService = openApiValidationService;

    this.serviceInterfaceIndexUri = serviceInterfaceProperties.getIndexUri();
    this.parsingMode = serviceInterfaceProperties.getParsingMode();
  }

  @Override
  public Set<ApiInformation> fetchApiIndex() {
    var apis = serviceInterfaceRepositoryClient
      .get()
      .uri(serviceInterfaceIndexUri)
      .accept(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
      .retrieve()
      .body(ApiInformation[].class);

    if (isEmpty(apis)) {
      return emptySet();
    }

    return stream(apis).collect(toSet());
  }

  @Override
  public ApiInformation validateApiInformation(ApiInformation apiInformation) {
    return openApiValidationService.validateApiInformationFromIndex(
      apiInformation,
      parsingMode
    );
  }
}
