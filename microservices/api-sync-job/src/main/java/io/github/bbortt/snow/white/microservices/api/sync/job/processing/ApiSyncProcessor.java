/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.processing;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PUBLISHED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static java.lang.Thread.currentThread;
import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

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
import java.util.function.Predicate;
import java.util.function.Supplier;
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

  public Map<ApiLoadStatus, Long> process(
    Collection<Supplier<@Nullable ApiInformation>> suppliers,
    Predicate<ApiInformation> consumer
  ) throws InterruptedException {
    BlockingQueue<Supplier<ApiInformation>> queue = new ArrayBlockingQueue<>(
      queueCapacity
    );
    Map<ApiLoadStatus, AtomicLong> statusTracker = new ConcurrentHashMap<>();

    // Poison pill used to stop workers cleanly
    Supplier<ApiInformation> POISON = () -> null;

    try (var workers = Executors.newFixedThreadPool(workerCount)) {
      for (int i = 0; i < workerCount; i++) {
        workers.submit(() -> {
          try {
            while (true) {
              Supplier<ApiInformation> supplier = queue.take();

              // shutdown signal
              if (supplier == POISON) {
                return;
              }

              var apiInformation = supplier.get();
              if (isNull(apiInformation)) {
                apiInformation = ApiInformation.builder()
                  .build()
                  .withLoadStatus(UNLOADED);
              }

              if (consumer.test(apiInformation)) {
                apiInformation = apiInformation.withLoadStatus(PUBLISHED);
              }

              statusTracker
                .computeIfAbsent(apiInformation.getLoadStatus(), k ->
                  new AtomicLong()
                )
                .incrementAndGet();
            }
          } catch (InterruptedException e) {
            currentThread().interrupt();
          }
        });
      }

      for (Supplier<ApiInformation> supplier : suppliers) {
        queue.put(supplier);
      }

      for (int i = 0; i < workerCount; i++) {
        queue.put(POISON);
      }
    }

    return statusTracker
      .entrySet()
      .stream()
      .map(statusEntry ->
        entry(statusEntry.getKey(), statusEntry.getValue().get())
      )
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
