/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.processing;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesNf;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiSyncProcessorUnitTest {

  @Mock
  private ApiSyncJobProperties properties;

  private ApiSyncProcessor fixture;

  @BeforeEach
  void setup() {
    properties = mock(ApiSyncJobProperties.class);

    // Order matters here: the constructor reads these getters eagerly to cache them as final int fields,
    // so the stubs must be in place before construction.
    // @InjectMocks would construct the fixture before this @BeforeEach body runs,
    // breaking that ordering - manual construction is kept intentionally.
    when(properties.getMaxParallelSyncTasks()).thenReturn(3);
    when(properties.getWorkQueueCapacity()).thenReturn(10);

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

      UnaryOperator<ApiInformation> consumer = api -> {
        counter.incrementAndGet();
        return api.withLoadStatus(PUBLISHED);
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

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, publish());

      assertThat(result).containsEntry(PUBLISHED, 2L);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_035_INDEX_OUTAGE_DEFERS_TO_NEXT_CYCLE)
    void shouldNotCountApisWhichTheIndexDidNotAcceptAsPublished()
      throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOADED),
        () -> new ApiInformation().withLoadStatus(LOADED)
      );

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, api ->
        api.withLoadStatus(PUBLISH_DEFERRED)
      );

      assertThat(result)
        .doesNotContainKey(PUBLISHED)
        .containsEntry(PUBLISH_DEFERRED, 2L);
    }

    @Test
    @VerifiesNf(NfTraceables.NF_009_BOUNDED_SYNC_FAN_OUT_WITH_BACKPRESSURE)
    void shouldNeverExceedTheConfiguredNumberOfInFlightSpecifications()
      throws InterruptedException {
      // Ten times the worker count, and three times the queue capacity: the
      // listing side has to wait on the queue rather than the queue growing.
      var specificationCount = 30;

      AtomicInteger inFlight = new AtomicInteger();
      AtomicInteger peakInFlight = new AtomicInteger();

      List<Supplier<ApiInformation>> suppliers = IntStream.range(
        0,
        specificationCount
      )
        .mapToObj(
          i ->
            (Supplier<ApiInformation>) () -> {
              peakInFlight.accumulateAndGet(
                inFlight.incrementAndGet(),
                Math::max
              );

              try {
                Thread.sleep(5);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                inFlight.decrementAndGet();
              }

              return new ApiInformation().withLoadStatus(LOADED);
            }
        )
        .toList();

      Map<ApiLoadStatus, Long> result = fixture.process(suppliers, publish());

      assertThat(peakInFlight.get()).isPositive().isLessThanOrEqualTo(3);
      assertThat(result).containsEntry(PUBLISHED, (long) specificationCount);
    }

    @Test
    void shouldCountRejectedStatuses() throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(
        () -> new ApiInformation().withLoadStatus(LOAD_FAILED),
        () ->
          new ApiInformation().withLoadStatus(MANDATORY_INFORMATION_MISSING),
        () -> new ApiInformation().withLoadStatus(NO_SOURCE)
      );

      Map<ApiLoadStatus, Long> result = fixture.process(
        suppliers,
        UnaryOperator.identity()
      );

      assertThat(result)
        .containsEntry(LOAD_FAILED, 1L)
        .containsEntry(MANDATORY_INFORMATION_MISSING, 1L)
        .containsEntry(NO_SOURCE, 1L);
    }

    @Test
    void shouldHandleNullApiInformation() throws InterruptedException {
      List<Supplier<ApiInformation>> suppliers = List.of(() -> null);

      Map<ApiLoadStatus, Long> result = fixture.process(
        suppliers,
        UnaryOperator.identity()
      );

      assertThat(result).containsEntry(UNLOADED, 1L);
    }

    /**
     * Only a strict parsing mode makes a supplier raise, and then the cycle is
     * meant to abort - but the raise must not take its worker with it, or the
     * listing side would block on a queue nobody drains any more.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldAbortTheCycleWhenASupplierThrows() {
      var failure = new IllegalStateException("boom");

      Supplier<ApiInformation> failing = () -> {
        throw failure;
      };

      // Ten times the worker count, and three times the queue capacity: were the
      // raise to end its worker, the listing side would never get this far.
      List<Supplier<ApiInformation>> suppliers = Stream.concat(
        Stream.of(failing),
        IntStream.range(0, 30).mapToObj(
          i -> (Supplier<ApiInformation>) () -> new ApiInformation()
        )
      ).toList();

      assertThatThrownBy(() -> fixture.process(suppliers, publish())).isEqualTo(
        failure
      );
    }

    @Test
    void shouldHandleEmptySupplierList() throws InterruptedException {
      Map<ApiLoadStatus, Long> result = fixture.process(List.of(), publish());

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
        return api.withLoadStatus(PUBLISHED);
      });

      assertThat(calls.get()).isEqualTo(4);
    }

    private static UnaryOperator<ApiInformation> publish() {
      return api -> api.withLoadStatus(PUBLISHED);
    }
  }
}
