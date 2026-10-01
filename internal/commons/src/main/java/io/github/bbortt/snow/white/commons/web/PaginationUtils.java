/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;

@NoArgsConstructor(access = PRIVATE)
public final class PaginationUtils {

  public static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";

  /**
   * Builds the page request for a list read, resolving {@code sort} against the endpoint's own
   * vocabulary.
   *
   * @throws InvalidSortException if {@code sort} names a property or direction {@code definition}
   *     does not accept, or does not parse at all
   */
  public static Pageable toPageable(
    @Nullable Integer page,
    @Nullable Integer size,
    @Nullable String sort,
    SortDefinition definition
  ) {
    int safePage = page != null && page >= 0 ? page : 0;
    int safeSize = size != null && size > 0 ? size : 20;

    return PageRequest.of(safePage, safeSize, definition.toSort(sort));
  }

  public static <T> HttpHeaders generatePaginationHttpHeaders(Page<T> page) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_X_TOTAL_COUNT, Long.toString(page.getTotalElements()));
    return headers;
  }
}
