/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestMapper;
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
  private ApiIndexApi apiIndexApiMock;

  @Mock
  private ApiTestMapper apiTestMapperMock;

  private ApiIndexService fixture;

  @BeforeEach
  void beforeEach() {
    fixture = new ApiIndexService(apiIndexApiMock, apiTestMapperMock);
  }

  @Nested
  class FetchCompleteApiInformation {

    @Test
    void returnsSuccessWhenApiIsFound() {
      var apiTest = defaultApiTest();

      var response = mock(ResponseEntity.class);
      doReturn(OK).when(response).getStatusCode();

      var apiDetails = mock(GetAllApis200ResponseInner.class);
      doReturn(apiDetails).when(response).getBody();

      doReturn(response)
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo("serviceName", "apiName", "apiVersion");

      doReturn(apiTest).when(apiTestMapperMock).toApiTest(apiDetails);

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results)
        .hasSize(1)
        .first()
        .asInstanceOf(type(Success.class))
        .satisfies(
          success -> assertThat(success.apiTest()).isNotNull(),
          success ->
            assertThat(success.apiTest().getServiceName()).isEqualTo(
              "serviceName"
            ),
          success ->
            assertThat(success.apiTest().getApiName()).isEqualTo("apiName"),
          success ->
            assertThat(success.apiTest().getApiVersion()).isEqualTo(
              "apiVersion"
            )
        );
    }

    @Test
    void returnsFailureWhenApiIsNotIndexed() {
      var apiTest = defaultApiTest();

      var response = mock(ResponseEntity.class);
      doReturn(NOT_FOUND).when(response).getStatusCode();

      doReturn(response)
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo("serviceName", "apiName", "apiVersion");

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results)
        .hasSize(1)
        .first()
        .asInstanceOf(type(Failure.class))
        .extracting(Failure::errorMessage)
        .isEqualTo(
          "API { serviceName='serviceName', apiName='apiName', apiVersion='apiVersion' } not indexed!"
        );
    }

    @Test
    void returnsFailureWhenApiVersionIsNull() {
      var apiTest = defaultApiTest();

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      assertThat(results)
        .hasSize(1)
        .first()
        .asInstanceOf(type(Failure.class))
        .extracting(Failure::errorMessage)
        .asInstanceOf(STRING)
        .startsWith("Unexpected error while requesting API information:");
    }

    @Test
    void returnsFailureWhenApiClientThrowsException() {
      var apiTest = defaultApiTest();

      doThrow(new RuntimeException("connection refused"))
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo("serviceName", "apiName", "apiVersion");

      var results = fixture.fetchCompleteApiInformation(Set.of(apiTest));

      assertThat(results).hasSize(1);

      assertThat(results)
        .hasSize(1)
        .first()
        .asInstanceOf(type(Failure.class))
        .extracting(Failure::errorMessage)
        .isEqualTo(
          "Unexpected error while requesting API information: connection refused"
        );
    }

    @Test
    void returnsEmptySetWhenInputIsEmpty() {
      var results = fixture.fetchCompleteApiInformation(Set.of());

      assertThat(results).isEmpty();

      verifyNoInteractions(apiIndexApiMock);
      verifyNoInteractions(apiTestMapperMock);
    }

    @Test
    void returnsOneResultPerApiTest() {
      var apiTest1 = ApiTest.builder()
        .serviceName("service1")
        .apiName("api1")
        .apiVersion("1.0.0")
        .apiType(OPENAPI.getVal())
        .build();

      var apiTest2 = ApiTest.builder()
        .serviceName("service2")
        .apiName("api2")
        .apiVersion("2.0.0")
        .apiType(OPENAPI.getVal())
        .build();

      var successResponse = mock(ResponseEntity.class);
      doReturn(OK).when(successResponse).getStatusCode();

      var apiDetails = mock(GetAllApis200ResponseInner.class);
      doReturn(apiDetails).when(successResponse).getBody();

      var notFoundResponse = mock(ResponseEntity.class);
      doReturn(NOT_FOUND).when(notFoundResponse).getStatusCode();

      doReturn(successResponse)
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo("service1", "api1", "1.0.0");

      doReturn(notFoundResponse)
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo("service2", "api2", "2.0.0");

      doReturn(apiTest1).when(apiTestMapperMock).toApiTest(apiDetails);

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
