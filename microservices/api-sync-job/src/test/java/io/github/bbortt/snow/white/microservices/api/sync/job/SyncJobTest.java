/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import io.github.bbortt.snow.white.microservices.api.sync.job.processing.ApiSyncProcessor;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncJobTest {

  @Mock
  private ApiCatalogService apiCatalogService1;

  @Mock
  private ApiCatalogService apiCatalogService2;

  @Mock
  private ApiSyncProcessor apiSyncProcessor;

  @Mock
  private CachingService cachingService;

  private SyncJob fixture;

  @Captor
  private ArgumentCaptor<List<Supplier<ApiInformation>>> suppliersCaptor;

  @Captor
  private ArgumentCaptor<Predicate<ApiInformation>> predicateCaptor;

  @BeforeEach
  void setup() {
    fixture = new SyncJob(
      List.of(apiCatalogService1, apiCatalogService2),
      apiSyncProcessor,
      cachingService
    );
  }

  @Nested
  class ConstructorTest {

    @Test
    void shouldAssignAllFields() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class SyncCatalogTest {

    @BeforeEach
    void setup() throws InterruptedException {
      fixture = new SyncJob(
        List.of(apiCatalogService1, apiCatalogService2),
        apiSyncProcessor,
        cachingService
      );

      // default processor behaviour
      when(apiSyncProcessor.process(any(), any())).thenReturn(
        Map.of(LOADED, 0L)
      );
    }

    @Test
    void shouldCollectAllSuppliersAndDelegateToProcessor()
      throws InterruptedException {
      Supplier<ApiInformation> s1 = mock();
      Supplier<ApiInformation> s2 = mock();
      Supplier<ApiInformation> s3 = mock();

      when(apiCatalogService1.getApiSpecificationLoaders()).thenReturn(
        List.of(s1, s2)
      );

      when(apiCatalogService2.getApiSpecificationLoaders()).thenReturn(
        List.of(s3)
      );

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(
        suppliersCaptor.capture(),
        predicateCaptor.capture()
      );

      assertThat(suppliersCaptor.getValue()).containsExactly(s1, s2, s3);
    }

    @Test
    void shouldPublishLoadedApisViaProcessorCallback()
      throws InterruptedException {
      ApiInformation api = new ApiInformation().withLoadStatus(LOADED);

      when(apiCatalogService1.getApiSpecificationLoaders()).thenReturn(
        List.of(() -> api)
      );

      when(apiCatalogService2.getApiSpecificationLoaders()).thenReturn(
        emptyList()
      );

      when(cachingService.apiInformationIndexed(api)).thenReturn(false);

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(
        suppliersCaptor.capture(),
        predicateCaptor.capture()
      );

      Predicate<ApiInformation> callback = predicateCaptor.getValue();

      boolean result = callback.test(api);

      assertThat(result).isTrue();

      verify(cachingService).apiInformationIndexed(api);
      verify(cachingService).publishApiInformation(api);
    }

    @Test
    void shouldSkipAlreadyIndexedApis() throws InterruptedException {
      ApiInformation api = new ApiInformation().withLoadStatus(LOADED);

      when(apiCatalogService1.getApiSpecificationLoaders()).thenReturn(
        List.of(() -> api)
      );

      when(cachingService.apiInformationIndexed(api)).thenReturn(true);

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(
        suppliersCaptor.capture(),
        predicateCaptor.capture()
      );

      Predicate<ApiInformation> callback = predicateCaptor.getValue();

      boolean result = callback.test(api);

      assertThat(result).isFalse();

      verify(cachingService).apiInformationIndexed(api);
      verify(cachingService, never()).publishApiInformation(any());
    }

    @Test
    void shouldSkipNonLoadedApis() throws InterruptedException {
      ApiInformation api = new ApiInformation().withLoadStatus(
        ApiLoadStatus.LOAD_FAILED
      );

      when(apiCatalogService1.getApiSpecificationLoaders()).thenReturn(
        List.of(() -> api)
      );

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(
        suppliersCaptor.capture(),
        predicateCaptor.capture()
      );

      Predicate<ApiInformation> callback = predicateCaptor.getValue();

      boolean result = callback.test(api);

      assertThat(result).isFalse();

      verifyNoInteractions(cachingService);
    }

    @Test
    void shouldHandleNullApiInformation() throws InterruptedException {
      when(apiCatalogService1.getApiSpecificationLoaders()).thenReturn(
        List.of(() -> null)
      );

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(
        suppliersCaptor.capture(),
        predicateCaptor.capture()
      );

      Predicate<ApiInformation> callback = predicateCaptor.getValue();

      boolean result = callback.test(null);

      assertThat(result).isFalse();

      verifyNoInteractions(cachingService);
    }

    @Test
    void shouldHandleEmptyCatalogServices() throws InterruptedException {
      fixture = new SyncJob(emptyList(), apiSyncProcessor, cachingService);

      fixture.syncCatalog();

      verify(apiSyncProcessor).process(eq(emptyList()), any());
    }
  }
}
