/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

class SortDefinitionTest {

  private static final SortDefinition DEFINITION = SortDefinition.builder()
    .sortable("initiatedAt", "createdAt")
    .sortable("qualityGateConfigName")
    .sortable("calculationId")
    .defaultOrder(DESC, "initiatedAt")
    .tiebreaker("calculationId")
    .build();

  @Nested
  class ToSortTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    @VerifiesSw(SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT)
    void shouldReturnTheDefaultOrderWhenNoPreferenceIsExpressed(String sort) {
      assertThat(DEFINITION.toSort(sort)).containsExactly(
        new Sort.Order(DESC, "createdAt"),
        new Sort.Order(ASC, "calculationId")
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES)
    void shouldTranslateThePublishedNameToItsEntityAttribute() {
      assertThat(DEFINITION.toSort("initiatedAt,asc")).containsExactly(
        new Sort.Order(ASC, "createdAt"),
        new Sort.Order(ASC, "calculationId")
      );
    }

    /**
     * The published name is the whole vocabulary: {@code createdAt} backs {@code initiatedAt} but is
     * not a second spelling of it, and the rejection does not offer it as one.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES)
    void shouldRejectTheEntityNameBehindAPublishedProperty() {
      assertThatExceptionOfType(InvalidSortException.class)
        .isThrownBy(() -> DEFINITION.toSort("createdAt,asc"))
        .withMessageEndingWith(
          "one of: calculationId, initiatedAt, qualityGateConfigName."
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "ASC", "Asc", " asc " })
    @VerifiesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
    void shouldAcceptADirectionInAnyCaseOrPadding(String direction) {
      assertThat(DEFINITION.toSort("initiatedAt," + direction)).startsWith(
        new Sort.Order(ASC, "createdAt")
      );
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "initiatedAt",
        "initiatedAt,asc,calculationId,desc",
        "initiatedAt,sideways",
        "unknown,asc",
        ",asc",
      }
    )
    @VerifiesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
    void shouldRejectASortItCannotHonour(String sort) {
      assertThatExceptionOfType(InvalidSortException.class)
        .isThrownBy(() -> DEFINITION.toSort(sort))
        .satisfies(ex -> assertThat(ex.getSort()).isEqualTo(sort));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
    void shouldNameThePublishedPropertiesAlphabeticallyInTheRejection() {
      assertThatExceptionOfType(InvalidSortException.class)
        .isThrownBy(() -> DEFINITION.toSort("unknown,asc"))
        .withMessage(
          "Cannot sort by 'unknown,asc'. Expected 'property,(asc|desc)' with property one of: calculationId, initiatedAt, qualityGateConfigName."
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT)
    void shouldNotRepeatTheTiebreakerTheRequestedOrderAlreadyCovers() {
      assertThat(DEFINITION.toSort("calculationId,desc")).containsExactly(
        new Sort.Order(DESC, "calculationId")
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT)
    void shouldAppendEveryTiebreakerProperty() {
      SortDefinition composite = SortDefinition.builder()
        .sortable("serviceName", "otelServiceName")
        .sortable("apiName")
        .sortable("apiVersion")
        .defaultOrder(ASC, "serviceName")
        .tiebreaker("serviceName", "apiName", "apiVersion")
        .build();

      assertThat(composite.toSort("apiName,desc")).containsExactly(
        new Sort.Order(DESC, "apiName"),
        new Sort.Order(ASC, "otelServiceName"),
        new Sort.Order(ASC, "apiVersion")
      );
    }
  }

  @Nested
  class BuilderTest {

    @Test
    void shouldRefuseADefinitionWithoutADefaultOrder() {
      var builder = SortDefinition.builder()
        .sortable("name")
        .tiebreaker("name");

      assertThatIllegalStateException()
        .isThrownBy(builder::build)
        .withMessageContaining("default order");
    }

    @Test
    void shouldRefuseADefinitionWithoutATiebreaker() {
      var builder = SortDefinition.builder()
        .sortable("name")
        .defaultOrder(ASC, "name");

      assertThatIllegalStateException()
        .isThrownBy(builder::build)
        .withMessageContaining("tiebreaker");
    }

    @Test
    void shouldRefuseADefaultOrderOnAPropertyItDoesNotPublish() {
      var builder = SortDefinition.builder().sortable("name");

      assertThatIllegalStateException()
        .isThrownBy(() -> builder.defaultOrder(ASC, "createdAt"))
        .withMessageContaining("'createdAt' is not a sortable property");
    }

    @Test
    void shouldRefuseATiebreakerOnAPropertyItDoesNotPublish() {
      var builder = SortDefinition.builder().sortable("name");

      assertThatIllegalStateException()
        .isThrownBy(() -> builder.tiebreaker("id"))
        .withMessageContaining("'id' is not a sortable property");
    }
  }
}
