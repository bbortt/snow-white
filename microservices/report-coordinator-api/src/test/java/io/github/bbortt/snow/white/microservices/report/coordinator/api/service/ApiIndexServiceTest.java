/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.model.ApiDetails;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTypeMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ApiIndexService.ValidationResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ApiIndexService.ValidationResult.Failure;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ApiIndexService.ValidationResult.Success;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ApiIndexServiceTest {

  @Mock
  private ApiIndexApi apiIndexApi;

  @Mock
  private ApiTypeMapper apiTypeMapper;

  private ApiIndexService fixture;

  @BeforeEach
  void beforeEach() {
    fixture = new ApiIndexService(apiIndexApi, apiTypeMapper);
  }

  @Nested
  class FetchCompleteApiInformation {

    @Test
    void returnsSuccessWhenApiIsFound() {
      var apiTest = new ApiTest()
        .withServiceName("service")
        .withApiName("api")
        .withApiVersion("1.0.0");

      var apiDetails = mock(ApiDetails.class);
      doReturn("service").when(apiDetails).getServiceName();
      doReturn("api").when(apiDetails).getApiName();
      doReturn("1.0.0").when(apiDetails).getApiVersion();
      doReturn(null).when(apiDetails).getApiType();

      var response = mock(ResponseEntity.class);
      doReturn(OK).when(response).getStatusCode();
      doReturn(apiDetails).when(response).getBody();

      doReturn(response)
        .when(apiIndexApi)
        .getApiDetailsWithHttpInfo("service", "api", "1.0.0");

      doReturn(null).when(apiTypeMapper).toEntity(null);

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      var result = results.iterator().next();
      assertThat(result).isInstanceOf(Success.class);

      var success = (Success) result;
      assertThat(success.apiTest().getServiceName()).isEqualTo("service");
      assertThat(success.apiTest().getApiName()).isEqualTo("api");
      assertThat(success.apiTest().getApiVersion()).isEqualTo("1.0.0");
    }

    @Test
    void returnsFailureWhenApiIsNotIndexed() {
      var apiTest = new ApiTest()
        .withServiceName("service")
        .withApiName("api")
        .withApiVersion("2.0.0");

      var response = mock(ResponseEntity.class);
      doReturn(NOT_FOUND).when(response).getStatusCode();

      doReturn(response)
        .when(apiIndexApi)
        .getApiDetailsWithHttpInfo("service", "api", "2.0.0");

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      var result = results.iterator().next();
      assertThat(result).isInstanceOf(Failure.class);
      assertThat(((Failure) result).errorMessage()).isEqualTo(
        "API { serviceName='service', apiName='api', apiVersion='2.0.0' } not indexed!"
      );
    }

    @Test
    void returnsFailureWhenApiVersionIsNull() {
      var apiTest = new ApiTest()
        .withServiceName("service")
        .withApiName("api")
        .withApiVersion(null);

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      var result = results.iterator().next();
      assertThat(result).isInstanceOf(Failure.class);
      assertThat(((Failure) result).errorMessage()).startsWith(
        "Unexpected error while requesting API information:"
      );
    }

    @Test
    void returnsFailureWhenApiClientThrowsException() {
      var apiTest = new ApiTest()
        .withServiceName("service")
        .withApiName("api")
        .withApiVersion("3.0.0");

      doThrow(new RuntimeException("connection refused"))
        .when(apiIndexApi)
        .getApiDetailsWithHttpInfo("service", "api", "3.0.0");

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      var result = results.iterator().next();
      assertThat(result).isInstanceOf(Failure.class);
      assertThat(((Failure) result).errorMessage()).isEqualTo(
        "Unexpected error while requesting API information: connection refused"
      );
    }

    @Test
    void returnsEmptySetWhenInputIsEmpty() {
      var results = fixture.fetchCompleteApiInformation(Set.of());

      assertThat(results).isEmpty();
    }

    @Test
    void returnsOneResultPerApiTest() {
      var apiTest1 = new ApiTest()
        .withServiceName("service1")
        .withApiName("api1")
        .withApiVersion("1.0.0");

      var apiTest2 = new ApiTest()
        .withServiceName("service2")
        .withApiName("api2")
        .withApiVersion("2.0.0");

      var apiDetails = mock(ApiDetails.class);
      doReturn("service1").when(apiDetails).getServiceName();
      doReturn("api1").when(apiDetails).getApiName();
      doReturn("1.0.0").when(apiDetails).getApiVersion();
      doReturn(null).when(apiDetails).getApiType();

      var successResponse = mock(ResponseEntity.class);
      doReturn(OK).when(successResponse).getStatusCode();
      doReturn(apiDetails).when(successResponse).getBody();

      var notFoundResponse = mock(ResponseEntity.class);
      doReturn(NOT_FOUND).when(notFoundResponse).getStatusCode();

      doReturn(successResponse)
        .when(apiIndexApi)
        .getApiDetailsWithHttpInfo("service1", "api1", "1.0.0");

      doReturn(notFoundResponse)
        .when(apiIndexApi)
        .getApiDetailsWithHttpInfo("service2", "api2", "2.0.0");

      doReturn(null).when(apiTypeMapper).toEntity(null);

      var results = fixture.fetchCompleteApiInformation(
        Set.of(apiTest1, apiTest2)
      );

      assertThat(results).hasSize(2);
      assertThat(results.stream().filter(ValidationResult::isSuccess)).hasSize(
        1
      );
      assertThat(results.stream().filter(ValidationResult::isFailure)).hasSize(
        1
      );
    }
  }
}
