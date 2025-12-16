/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import io.github.bbortt.snow.white.microservices.api.index.api.mapper.ApiReferenceMapper;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis500Response;
import io.github.bbortt.snow.white.microservices.api.index.service.ApiIndexService;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiIndexResource implements ApiIndexApi {

  private final ApiIndexService apiIndexService;
  private final ApiReferenceMapper apiReferenceMapper;

  @Override
  public ResponseEntity<@NonNull Void> ingestApi(
    GetAllApis200ResponseInner apiInformation
  ) {
    try {
      apiIndexService.persist(apiReferenceMapper.fromDto(apiInformation));
    } catch (ApiAlreadyIndexedException e) {
      return ResponseEntity.status(CONFLICT).build();
    }

    return ResponseEntity.status(CREATED).build();
  }

  @Override
  public ResponseEntity<@NonNull Void> checkApiExists(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    if (
      apiIndexService.hasApiByInformationBeenIndexed(
        otelServiceName,
        apiName,
        apiVersion
      )
    ) {
      return ResponseEntity.ok().build();
    }

    return ResponseEntity.notFound().build();
  }

  @Override
  public ResponseEntity<@NonNull List<GetAllApis200ResponseInner>> getAllApis(
    Integer page,
    Integer size,
    String sort
  ) {
    var ingestedApis = apiIndexService.findAllIngestedApis(
      toPageable(page, size, sort)
    );

    return ResponseEntity.ok()
      .headers(generatePaginationHttpHeaders(ingestedApis))
      .body(ingestedApis.stream().map(apiReferenceMapper::toDto).toList());
  }

  @Override
  public ResponseEntity getApiDetails(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    var optionalIngestedApi = apiIndexService.findIngestedApi(
      otelServiceName,
      apiName,
      apiVersion
    );

    if (optionalIngestedApi.isEmpty()) {
      return ResponseEntity.status(NOT_FOUND)
        .contentType(APPLICATION_JSON)
        .body(
          GetAllApis500Response.builder()
            .code(NOT_FOUND.getReasonPhrase())
            .message(
              "No API specification exists for the given service name, API name, and version."
            )
            .build()
        );
    }

    return ResponseEntity.ok(
      apiReferenceMapper.toDto(optionalIngestedApi.get())
    );
  }
}
