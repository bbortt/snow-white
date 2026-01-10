/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.Entity;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PagedBackstageEntitySpliterator implements Spliterator<Entity> {

  private final EntityApi backstageEntityApi;
  private final int pageSize;
  private int offset = 0;
  private List<Entity> currentPage;
  private int currentIndex = 0;

  public PagedBackstageEntitySpliterator(
    EntityApi backstageEntityApi,
    int pageSize
  ) {
    this.backstageEntityApi = backstageEntityApi;
    this.pageSize = pageSize;
  }

  @Override
  public boolean tryAdvance(Consumer<? super Entity> action) {
    if (currentPage == null || currentIndex >= currentPage.size()) {
      currentPage = backstageEntityApi
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          pageSize,
          offset,
          null,
          null,
          null,
          null,
          null
        )
        .getItems();
      offset += pageSize;
      currentIndex = 0;
    }

    if (currentPage.isEmpty()) {
      return false;
    }

    action.accept(currentPage.get(currentIndex++));

    return true;
  }

  @Override
  public Spliterator<Entity> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return ORDERED | IMMUTABLE;
  }
}
