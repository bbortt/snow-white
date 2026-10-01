/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

class PaginationUtilsTest {

  private static final SortDefinition DEFINITION = SortDefinition.builder()
    .sortable("initiatedAt", "createdAt")
    .sortable("calculationId")
    .defaultOrder(DESC, "initiatedAt")
    .tiebreaker("calculationId")
    .build();

  @Nested
  class ToPageableTest {

    @Test
    void shouldReturnDefaultPageAndSizeForNullInputs() {
      Pageable pageable = PaginationUtils.toPageable(
        null,
        null,
        null,
        DEFINITION
      );

      assertThat(pageable.getPageNumber()).isZero();
      assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldApplyPageAndSize() {
      Pageable pageable = PaginationUtils.toPageable(2, 50, null, DEFINITION);

      assertThat(pageable.getPageNumber()).isEqualTo(2);
      assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void shouldFallbackToDefaultsOnNegativeValues() {
      Pageable pageable = PaginationUtils.toPageable(-5, -10, null, DEFINITION);

      assertThat(pageable.getPageNumber()).isZero();
      assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldNeverReturnAnUnsortedPageable() {
      Pageable pageable = PaginationUtils.toPageable(0, 10, null, DEFINITION);

      assertThat(pageable.getSort().isSorted()).isTrue();
      assertThat(pageable.getSort()).containsExactly(
        new Sort.Order(DESC, "createdAt"),
        new Sort.Order(ASC, "calculationId")
      );
    }

    @Test
    void shouldResolveSortAgainstTheGivenDefinition() {
      Pageable pageable = PaginationUtils.toPageable(
        0,
        10,
        "initiatedAt,asc",
        DEFINITION
      );

      assertThat(pageable.getSort()).containsExactly(
        new Sort.Order(ASC, "createdAt"),
        new Sort.Order(ASC, "calculationId")
      );
    }

    @Test
    void shouldPropagateRejectionOfAnUnusableSort() {
      assertThatExceptionOfType(InvalidSortException.class).isThrownBy(() ->
        PaginationUtils.toPageable(0, 10, "unknown,asc", DEFINITION)
      );
    }
  }

  @Nested
  class GeneratePaginationHttpHeadersTest {

    @Test
    void shouldGenerateTotalCountHeader() {
      List<String> content = List.of("a", "b", "c");
      long totalElements = 123;
      Page<String> page = new PageImpl<>(
        content,
        PageRequest.of(0, 10),
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
