/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.client;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ApiIndexApiClientUnitTest {

  @Mock
  private ApiIndexApi apiIndexApiMock;

  private ApiIndexApiClient fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexApiClient();
    fixture.setApiIndexApi(apiIndexApiMock);
  }

  @Nested
  class CheckApiExistsWithHttpInfoTest {

    @Test
    void shouldCallUnderlyingApiIndexApi() {
      var serviceName = "serviceName";
      var apiName = "apiName";
      var apiVersion = "apiVersion";
      var includePrereleases = TRUE;

      fixture.checkApiExistsWithHttpInfo(
        serviceName,
        apiName,
        apiVersion,
        includePrereleases
      );

      verify(apiIndexApiMock).checkApiExistsWithHttpInfo(
        serviceName,
        apiName,
        apiVersion,
        includePrereleases
      );
    }
  }

  @Nested
  class RecoverCheckApiExistsWithHttpInfoTest {

    @Mock
    private Exception exceptionMock;

    @Mock
    private ApiInformation apiInformationMock;

    @Test
    void shouldAlwaysReturnNotFound() {
      assertThat(
        fixture.recoverCheckApiExistsWithHttpInfo(
          exceptionMock,
          "otelServiceName",
          "apiName",
          "apiVersion",
          TRUE
        )
      )
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(NOT_FOUND);
    }
  }

  @Nested
  class IngestApiWithHttpInfoTest {

    @Test
    void shouldCallUnderlyingApiIndexApi() {
      var dto = mock(GetAllApis200ResponseInner.class);

      fixture.ingestApiWithHttpInfo(dto);

      verify(apiIndexApiMock).ingestApiWithHttpInfo(dto);
    }
  }

  @Nested
  class RecoverIngestApiWithHttpInfoTest {

    @Mock
    private Exception exceptionMock;

    @Mock
    private GetAllApis200ResponseInner dtoMock;

    @Test
    void shouldAlwaysReturnOk() {
      assertThat(fixture.recoverIngestApiWithHttpInfo(exceptionMock, dtoMock))
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(OK);
    }
  }
}
