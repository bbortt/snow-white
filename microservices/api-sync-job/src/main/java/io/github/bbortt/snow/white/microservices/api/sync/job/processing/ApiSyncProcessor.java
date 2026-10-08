/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.processing;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static java.lang.Thread.currentThread;
import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesNf;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiSyncProcessor {

  private final int workerCount;
  private final int queueCapacity;

  public ApiSyncProcessor(ApiSyncJobProperties apiSyncJobProperties) {
    this.workerCount = apiSyncJobProperties.getMaxParallelSyncTasks();
    this.queueCapacity = apiSyncJobProperties.getWorkQueueCapacity();
  }

  /**
   * At most {@code workerCount} specifications are in flight at once, handed
   * over through a queue of {@code queueCapacity}. The listing side blocks on a
   * full queue rather than the queue growing, so the footprint is a property of
   * the configuration rather than of the repository's size - and every listed
   * specification is still processed, only later. Bounding the fan-out never
   * discards work; only a supplier that raises does, by aborting the cycle.
   */
  @RealizesNf(NfTraceables.NF_009_BOUNDED_SYNC_FAN_OUT_WITH_BACKPRESSURE)
  @RealizesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
  public Map<ApiLoadStatus, Long> process(
    Collection<Supplier<@Nullable ApiInformation>> suppliers,
    UnaryOperator<ApiInformation> apiInformationPublisher
  ) throws InterruptedException {
    BlockingQueue<Supplier<ApiInformation>> queue = new ArrayBlockingQueue<>(
      queueCapacity
    );
    Map<ApiLoadStatus, AtomicLong> statusTracker = new ConcurrentHashMap<>();
    AtomicReference<RuntimeException> firstFailure = new AtomicReference<>();

    // Poison pill used to stop workers cleanly
    Supplier<ApiInformation> poisonPill = () -> null;

    try (var workers = Executors.newFixedThreadPool(workerCount)) {
      for (int i = 0; i < workerCount; i++) {
        workers.submit(() ->
          runWorker(
            queue,
            poisonPill,
            apiInformationPublisher,
            statusTracker,
            firstFailure
          )
        );
      }

      for (Supplier<ApiInformation> supplier : suppliers) {
        queue.put(supplier);
      }

      for (int i = 0; i < workerCount; i++) {
        queue.put(poisonPill);
      }
    }

    var failure = firstFailure.get();
    if (nonNull(failure)) {
      throw failure;
    }

    return toStatusCounts(statusTracker);
  }

  /**
   * A supplier that raises - which only a strict parsing mode makes it do -
   * aborts the cycle: the failure is remembered, the remaining items are taken
   * off the queue without being processed so the listing side never blocks on a
   * full queue, and {@link #process} rethrows once the workers are done.
   */
  @RealizesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
  private void runWorker(
    BlockingQueue<Supplier<ApiInformation>> queue,
    Supplier<ApiInformation> poisonPill,
    UnaryOperator<ApiInformation> apiInformationPublisher,
    Map<ApiLoadStatus, AtomicLong> statusTracker,
    AtomicReference<RuntimeException> firstFailure
  ) {
    try {
      while (true) {
        Supplier<ApiInformation> supplier = queue.take();

        // shutdown signal
        if (supplier == poisonPill) {
          return;
        }

        if (nonNull(firstFailure.get())) {
          continue;
        }

        trackOrRecordFailure(
          supplier,
          apiInformationPublisher,
          statusTracker,
          firstFailure
        );
      }
    } catch (InterruptedException _) {
      currentThread().interrupt();
    }
  }

  /**
   * Only the first failure is remembered, and it is the one {@link #process}
   * rethrows. A second worker raising for a different reason at the same moment
   * would otherwise vanish, leaving an operator debugging the aborted cycle with
   * one of two causes.
   */
  private void trackOrRecordFailure(
    Supplier<ApiInformation> supplier,
    UnaryOperator<ApiInformation> apiInformationPublisher,
    Map<ApiLoadStatus, AtomicLong> statusTracker,
    AtomicReference<RuntimeException> firstFailure
  ) {
    try {
      trackSuppliedApiInformation(
        supplier,
        apiInformationPublisher,
        statusTracker
      );
    } catch (RuntimeException e) {
      if (!firstFailure.compareAndSet(null, e)) {
        logger.warn("Further failure while aborting the cycle:", e);
      }
    }
  }

  private void trackSuppliedApiInformation(
    Supplier<ApiInformation> supplier,
    UnaryOperator<ApiInformation> apiInformationPublisher,
    Map<ApiLoadStatus, AtomicLong> statusTracker
  ) {
    var apiInformation = supplier.get();
    if (isNull(apiInformation)) {
      apiInformation = ApiInformation.builder()
        .build()
        .withLoadStatus(UNLOADED);
    }

    var publishedApiInformation = apiInformationPublisher.apply(apiInformation);

    statusTracker
      .computeIfAbsent(publishedApiInformation.getLoadStatus(), k ->
        new AtomicLong()
      )
      .incrementAndGet();
  }

  private static Map<ApiLoadStatus, Long> toStatusCounts(
    Map<ApiLoadStatus, AtomicLong> statusTracker
  ) {
    return statusTracker
      .entrySet()
      .stream()
      .map(statusEntry ->
        entry(statusEntry.getKey(), statusEntry.getValue().get())
      )
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
