/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

@NoArgsConstructor(access = PRIVATE)
public final class PaginationUtils {

  public static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";

  public static Pageable toPageable(
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

  public static <T> HttpHeaders generatePaginationHttpHeaders(Page<T> page) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_X_TOTAL_COUNT, Long.toString(page.getTotalElements()));
    return headers;
  }
}
