package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasLength;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ServiceInterfaceCatalogService implements ApiCatalogService {

  private final RestClient serviceInterfaceRepositoryClient;
  private final String serviceInterfaceIndexUri;

  private final ParsingMode parsingMode;

  public ServiceInterfaceCatalogService(
    RestClient.Builder restCLientBuilder,
    ApiSyncJobProperties apiSyncJobProperties
  ) {
    this.serviceInterfaceRepositoryClient = restCLientBuilder
      .baseUrl(apiSyncJobProperties.getServiceInterface().getBaseUrl())
      .build();
    this.serviceInterfaceIndexUri = apiSyncJobProperties
      .getServiceInterface()
      .getIndexUri();

    this.parsingMode = apiSyncJobProperties
      .getServiceInterface()
      .getParsingMode();
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
  public ApiInformation validateApiInformationFromIndex(
    ApiInformation apiInformation
  ) {
    if (!hasLength(apiInformation.getSourceUrl())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without source URL: %s!",
            apiInformation.getTitle()
          )
        );
      } else {
        return apiInformation.withLoadStatus(NO_SOURCE);
      }
    }

    if (isNull(apiInformation.getApiType())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without type definition: %s!",
            apiInformation.getTitle()
          )
        );
      } else {
        return apiInformation.withLoadStatus(LOAD_FAILED);
      }
    }

    return apiInformation.withLoadStatus(LOADED);
  }
}
