package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.ApiLoadStatus.NO_SOURCE;
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
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
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
  public Set<Api> fetchApiIndex() {
    var apis = serviceInterfaceRepositoryClient
      .get()
      .uri(serviceInterfaceIndexUri)
      .accept(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
      .retrieve()
      .body(Api[].class);

    if (isEmpty(apis)) {
      return emptySet();
    }

    return stream(apis).collect(toSet());
  }

  @Override
  public Api validateApiInformationFromIndex(Api api) {
    if (!hasLength(api.getSourceUrl())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without source URL: %s!",
            api.getTitle()
          )
        );
      } else {
        return api.withLoadStatus(NO_SOURCE);
      }
    }

    if (isNull(api.getApiType())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without type definition: %s!",
            api.getTitle()
          )
        );
      } else {
        return api.withLoadStatus(LOAD_FAILED);
      }
    }

    return api.withLoadStatus(LOADED);
  }
}
