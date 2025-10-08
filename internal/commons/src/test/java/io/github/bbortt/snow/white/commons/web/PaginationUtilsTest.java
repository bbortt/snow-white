/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

class PaginationUtilsTest {

  @Nested
  class toPageable {

    @Test
    void shouldReturnDefaultPageableForNullInputs() {
      Pageable pageable = PaginationUtils.toPageable(null, null, null);

      Assertions.assertThat(pageable.getPageNumber()).isZero();
      Assertions.assertThat(pageable.getPageSize()).isEqualTo(20);
      Assertions.assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldApplyPageAndSize() {
      Pageable pageable = PaginationUtils.toPageable(2, 50, null);

      Assertions.assertThat(pageable.getPageNumber()).isEqualTo(2);
      Assertions.assertThat(pageable.getPageSize()).isEqualTo(50);
      Assertions.assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldFallbackToDefaultsOnNegativeValues() {
      Pageable pageable = PaginationUtils.toPageable(-5, -10, null);

      Assertions.assertThat(pageable.getPageNumber()).isZero();
      Assertions.assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldParseAscendingSort() {
      Pageable pageable = PaginationUtils.toPageable(0, 10, "name,asc");

      Assertions.assertThat(pageable.getSort().getOrderFor("name"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldParseDescendingSort() {
      Pageable pageable = PaginationUtils.toPageable(0, 10, "createdAt,desc");

      Assertions.assertThat(pageable.getSort().getOrderFor("createdAt"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldIgnoreMalformedSortString() {
      Pageable pageable = PaginationUtils.toPageable(0, 10, "badformat");

      Assertions.assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldIgnoreEmptySort() {
      Pageable pageable = PaginationUtils.toPageable(0, 10, "   ");

      Assertions.assertThat(pageable.getSort().isUnsorted()).isTrue();
    }
  }

  @Nested
  class GeneratePaginationHttpHeaders {

    @Test
    void shouldGenerateTotalCountHeader() {
      List<String> content = List.of("a", "b", "c");
      long totalElements = 123;
      Page<String> page = new PageImpl<>(
        content,
        org.springframework.data.domain.PageRequest.of(0, 10),
        totalElements
      );

      HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(page);

      assertThat(headers).isNotNull();
      assertThat(headers.getFirst("X-Total-Count")).isEqualTo("123");
      assertThat(headers.headerNames()).containsExactly("X-Total-Count");
    }

    @Test
    void shouldHandleEmptyPage() {
      Page<String> emptyPage = Page.empty();

      HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(
        emptyPage
      );

      assertThat(headers.getFirst("X-Total-Count")).isEqualTo("0");
      assertThat(headers.headerNames()).containsExactly("X-Total-Count");
    }
  }
}
