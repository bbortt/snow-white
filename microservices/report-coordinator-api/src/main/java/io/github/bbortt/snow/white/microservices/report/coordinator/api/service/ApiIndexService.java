/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiIndexService {

  private final ApiIndexApi apiIndexApi;
  private final ApiTestMapper apiTestMapper;

  public Set<ValidationResult> fetchCompleteApiInformation(
    Set<ApiTest> apiTests
  ) {
    return apiTests
      .parallelStream()
      .map(this::validate)
      .collect(toUnmodifiableSet());
  }

  private ValidationResult validate(ApiTest apiTest) {
    try {
      var apiVersion = requireNonNull(apiTest.getApiVersion(), () ->
        "apiVersion must not be null for API { serviceName='%s', apiName='%s' }".formatted(
          apiTest.getServiceName(),
          apiTest.getApiName()
        )
      );

      var response = apiIndexApi.getApiDetailsWithHttpInfo(
        apiTest.getServiceName(),
        apiTest.getApiName(),
        apiVersion
      );

      return toValidationResult(apiTest, response);
    } catch (Exception e) {
      var rootCause = getRootCause(e);
      var message = rootCause != null ? rootCause.getMessage() : e.getMessage();
      return ValidationResult.failure(
        "Unexpected error while requesting API information: %s".formatted(
          message
        )
      );
    }
  }

  private ValidationResult toValidationResult(
    ApiTest apiTest,
    ResponseEntity<GetAllApis200ResponseInner> response
  ) {
    if (NOT_FOUND.equals(response.getStatusCode())) {
      return ValidationResult.failure(
        "API { serviceName='%s', apiName='%s', apiVersion='%s' } not indexed!".formatted(
          apiTest.getServiceName(),
          apiTest.getApiName(),
          apiTest.getApiVersion()
        )
      );
    }

    var body = requireNonNull(
      response.getBody(),
      "API response body must not be null"
    );

    return ValidationResult.success(apiTestMapper.toApiTest(body));
  }

  public sealed interface ValidationResult
    permits ValidationResult.Success, ValidationResult.Failure
  {
    record Success(ApiTest apiTest) implements ValidationResult {}

    record Failure(String errorMessage) implements ValidationResult {}

    static ValidationResult success(ApiTest apiTest) {
      return new Success(apiTest);
    }

    static ValidationResult failure(String errorMessage) {
      return new Failure(errorMessage);
    }

    default boolean isSuccess() {
      return this instanceof Success;
    }

    default boolean isFailure() {
      return this instanceof Failure;
    }
  }
}
