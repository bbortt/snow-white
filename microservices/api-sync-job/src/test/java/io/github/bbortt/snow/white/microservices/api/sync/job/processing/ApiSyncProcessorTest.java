/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.processing;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.MANDATORY_INFORMATION_MISSING;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PUBLISHED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiSyncProcessorTest {

  @Mock
  private ApiSyncJobProperties properties;

  private ApiSyncProcessor fixture;

  @BeforeEach
  void setup() {
    properties = Mockito.mock(ApiSyncJobProperties.class);

    Mockito.when(properties.getMaxParallelSyncTasks()).thenReturn(3);
    Mockito.when(properties.getWorkQueueCapacity()).thenReturn(10);

    fixture = new ApiSyncProcessor(properties);
  }

  @Nested
  class ConstructorTest {

    @Test
    void shouldAssignAllFields() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class ProcessTest {

    @Test
    void shouldProcessAllSuppliers() throws InterruptedException {
      AtomicInteger counter = new AtomicInteger();

      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED)
      );

      Predicate<ApiInformation> consumer = api -> {
        counter.incrementAndGet();
        return true;
      };

      fixture.process(suppliers, consumer);

      assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void shouldCountPublishedApis() throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED)
      );

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, api -> true);

      assertThat(result).containsEntry(PUBLISHED, 2L);
    }

    @Test
    void shouldCountRejectedStatuses() throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOAD_FAILED),
        () ->
          new ApiInformation().withLoadStatus(MANDATORY_INFORMATION_MISSING),
        () -> new ApiInformation().withLoadStatus(NO_SOURCE)
      );

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, api ->
        false
      );

      assertThat(result)
        .containsEntry(LOAD_FAILED, 1L)
        .containsEntry(MANDATORY_INFORMATION_MISSING, 1L)
        .containsEntry(NO_SOURCE, 1L);
    }

    @Test
    void shouldHandleNullApiInformation() throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(() -> null);

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, api ->
        false
      );

      assertThat(result).containsEntry(UNLOADED, 1L);
    }

    @Test
    void shouldContinueWhenSupplierThrows() {
      Supplier<ApiInformation> failing = () -> {
        throw new RuntimeException("boom");
      };

      Supplier<ApiInformation> succeeding = () ->
        new ApiInformation().withLoadStatus(LOADED);

      AtomicInteger consumed = new AtomicInteger();

      Predicate<ApiInformation> consumer = api -> {
        if (api != null) {
          consumed.incrementAndGet();
        }
        return true;
      };

      assertDoesNotThrow(() ->
        fixture.process(List.of(failing, succeeding), consumer)
      );

      assertThat(consumed.get()).isEqualTo(1);
    }

    @Test
    void shouldHandleEmptySupplierList() throws InterruptedException {
      Map<ApiLoadStatus, Long> result = fixture.process(List.of(), api -> true);

      assertThat(result).isEmpty();
    }

    @Test
    void shouldInvokeConsumerExactlyOncePerSupplier()
      throws InterruptedException {
      AtomicInteger calls = new AtomicInteger();

      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED)
      );

      fixture.process(suppliers, api -> {
        calls.incrementAndGet();
        return true;
      });

      assertThat(calls.get()).isEqualTo(4);
    }
  }
}
