/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import jakarta.annotation.Nullable;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

@NoArgsConstructor(access = PRIVATE)
final class PaginationUtils {

  @VisibleForTesting
  static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";

  static Pageable toPageable(
    @Nullable Integer page,
    @Nullable Integer size,
    @Nullable String sort
  ) {
    int safePage = (page != null && page >= 0) ? page : 0;
    int safeSize = (size != null && size > 0) ? size : 20;

    Sort sortObj = Sort.unsorted();

    if (sort != null && !sort.isBlank()) {
      String[] sortParts = sort.split(",");
      if (sortParts.length == 2) {
        String property = sortParts[0].trim();
        String direction = sortParts[1].trim().toLowerCase();
        if (direction.equals("asc")) {
          sortObj = Sort.by(Sort.Direction.ASC, property);
        } else if (direction.equals("desc")) {
          sortObj = Sort.by(Sort.Direction.DESC, property);
        }
      }
    }

    return PageRequest.of(safePage, safeSize, sortObj);
  }

  static <T> HttpHeaders generatePaginationHttpHeaders(Page<T> page) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_X_TOTAL_COUNT, Long.toString(page.getTotalElements()));
    return headers;
  }
}
