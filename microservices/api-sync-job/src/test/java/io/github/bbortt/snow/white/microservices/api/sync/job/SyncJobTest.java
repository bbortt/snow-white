/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class SyncJobTest {

  @Mock
  private ApiCatalogService apiCatalogService1;

  @Mock
  private ApiCatalogService apiCatalogService2;

  @Mock
  private CachingService cachingServiceMock;

  private SyncJob fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SyncJob(
      List.of(apiCatalogService1, apiCatalogService2),
      cachingServiceMock
    );
  }

  @Nested
  class SyncCatalogTest {

    private Triple<
      ApiInformation,
      ApiInformation,
      ApiInformation
    > prepareTwoApiCachingServices(ApiLoadStatus loadStatus) {
      ApiInformation api1 = new ApiInformation().withLoadStatus(loadStatus);
      ApiInformation api2 = new ApiInformation().withLoadStatus(loadStatus);
      ApiInformation api3 = new ApiInformation().withLoadStatus(loadStatus);

      doReturn(List.<Supplier<ApiInformation>>of(() -> api1, () -> api2))
        .when(apiCatalogService1)
        .getApiSpecificationLoaders();

      doReturn(singletonList((Supplier<ApiInformation>) () -> api3))
        .when(apiCatalogService2)
        .getApiSpecificationLoaders();

      return Triple.ofNonNull(api1, api2, api3);
    }

    @Test
    void shouldPublishAllLoadedNewApis() {
      var triple = prepareTwoApiCachingServices(LOADED);

      ApiInformation api1 = triple.getLeft();
      ApiInformation api2 = triple.getMiddle();
      ApiInformation api3 = triple.getRight();

      doReturn(false)
        .when(cachingServiceMock)
        .apiInformationIndexed(any(ApiInformation.class));

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verify(cachingServiceMock).apiInformationIndexed(api1);
      verify(cachingServiceMock).apiInformationIndexed(api2);
      verify(cachingServiceMock).apiInformationIndexed(api3);
      verify(cachingServiceMock).publishApiInformation(api1);
      verify(cachingServiceMock).publishApiInformation(api2);
      verify(cachingServiceMock).publishApiInformation(api3);
    }

    @Test
    void shouldSkipAlreadyIndexedApis() {
      var triple = prepareTwoApiCachingServices(LOADED);

      ApiInformation api1 = triple.getLeft();
      ApiInformation api2 = triple.getMiddle();
      ApiInformation api3 = triple.getRight();

      doReturn(true).when(cachingServiceMock).apiInformationIndexed(api1);
      doReturn(false).when(cachingServiceMock).apiInformationIndexed(api2);
      doReturn(true).when(cachingServiceMock).apiInformationIndexed(api3);

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verify(cachingServiceMock).apiInformationIndexed(api1);
      verify(cachingServiceMock).apiInformationIndexed(api2);
      verify(cachingServiceMock).apiInformationIndexed(api3);
      verify(cachingServiceMock, never()).publishApiInformation(api1);
      verify(cachingServiceMock).publishApiInformation(api2);
      verify(cachingServiceMock, never()).publishApiInformation(api3);
    }

    @ParameterizedTest
    @EnumSource(
      value = ApiLoadStatus.class,
      names = { "LOADED" },
      mode = EXCLUDE
    )
    void shouldSkipAllErroneousApis(ApiLoadStatus loadStatus) {
      prepareTwoApiCachingServices(loadStatus);

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verifyNoInteractions(cachingServiceMock);
    }

    @Test
    void shouldHandleEmptyCatalogServices() {
      fixture = new SyncJob(emptyList(), cachingServiceMock);

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verifyNoInteractions(cachingServiceMock);
    }

    @Test
    void shouldHandleEmptyCatalog() {
      doReturn(emptyList())
        .when(apiCatalogService1)
        .getApiSpecificationLoaders();
      doReturn(emptyList())
        .when(apiCatalogService2)
        .getApiSpecificationLoaders();

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verifyNoInteractions(cachingServiceMock);
    }

    @Test
    void shouldHandleNullishCatalog() {
      doReturn(singletonList((Supplier<ApiInformation>) () -> null))
        .when(apiCatalogService1)
        .getApiSpecificationLoaders();

      doReturn(emptyList())
        .when(apiCatalogService2)
        .getApiSpecificationLoaders();

      assertDoesNotThrow(() -> fixture.syncCatalog());

      verifyNoInteractions(cachingServiceMock);
    }
  }
}
