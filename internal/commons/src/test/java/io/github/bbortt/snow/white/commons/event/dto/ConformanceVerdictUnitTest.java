/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.CONFORMING_RESPONSE;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTERS;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTER_BYTES;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.bounded;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.nonConforming;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConformanceVerdictUnitTest {

  @Nested
  class ConformingTest {

    @Test
    void shouldBeConforming_withoutViolationPointers() {
      assertThat(CONFORMING_RESPONSE.conforming()).isTrue();
      assertThat(CONFORMING_RESPONSE.violationPointers()).isEmpty();
    }
  }

  @Nested
  class NonConformingTest {

    @Test
    void shouldKeepPointersInOrder() {
      var result = nonConforming(List.of("/properties/id", "/required"));

      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly(
        "/properties/id",
        "/required"
      );
    }

    @Test
    void shouldDropPointersBeyondTheLimit_andStayNonConforming() {
      var pointers = new ArrayList<String>();
      for (var i = 0; i < MAX_VIOLATION_POINTERS + 1; i++) {
        pointers.add("/properties/p" + i);
      }

      var result = nonConforming(pointers);

      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactlyElementsOf(
        pointers.subList(0, MAX_VIOLATION_POINTERS)
      );
    }

    @Test
    void shouldDropPointerOverTheByteBound_butKeepShorterOnes() {
      var overLong = "/" + "ä".repeat(300);
      assertThat(overLong.length()).isLessThan(MAX_VIOLATION_POINTER_BYTES);
      assertThat(overLong.getBytes(UTF_8)).hasSizeGreaterThan(
        MAX_VIOLATION_POINTER_BYTES
      );

      var result = nonConforming(
        List.of("/properties/id", overLong, "/required")
      );

      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly(
        "/properties/id",
        "/required"
      );
    }

    @Test
    void shouldKeepPointerOfExactlyTheByteBound() {
      var atTheBound =
        "/" + "ä".repeat((MAX_VIOLATION_POINTER_BYTES - 2) / 2) + "a";
      assertThat(atTheBound.getBytes(UTF_8)).hasSize(
        MAX_VIOLATION_POINTER_BYTES
      );

      var result = nonConforming(List.of(atTheBound));

      assertThat(result.violationPointers()).containsExactly(atTheBound);
    }

    @Test
    void shouldCollapseDuplicatePointers() {
      var result = nonConforming(
        List.of("/properties/id", "/required", "/properties/id")
      );

      assertThat(result.violationPointers()).containsExactly(
        "/properties/id",
        "/required"
      );
    }
  }

  @Nested
  class BoundedTest {

    @Test
    void shouldReturnEmptyList_whenGivenEmptyList() {
      assertThat(bounded(List.of())).isEmpty();
    }
  }
}
