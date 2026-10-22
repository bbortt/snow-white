/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiTestFindingUnitTest {

  private static ApiTestFinding.ApiTestFindingBuilder finding() {
    return ApiTestFinding.builder().specPointer("/paths/~1api").status(COVERED);
  }

  @Nested
  class EvidenceTest {

    @Test
    void shouldBeDecoupledFromTheListItWasBuiltFrom() {
      var mutableEvidence = new ArrayList<FindingEvidence>();
      mutableEvidence.add(new FindingEvidence("traceId", null));

      var result = finding().evidence(mutableEvidence).build();

      mutableEvidence.clear();

      assertThat(result.evidence()).containsExactly(
        new FindingEvidence("traceId", null)
      );
    }

    @Test
    void shouldBeUnmodifiable() {
      var result = finding()
        .evidence(List.of(new FindingEvidence("traceId", null)))
        .build();
      var evidence = result.evidence();
      var otherEvidence = new FindingEvidence("otherTraceId", null);

      assertThatThrownBy(() -> evidence.add(otherEvidence)).isInstanceOf(
        UnsupportedOperationException.class
      );
    }

    @Test
    void shouldRejectBeingUnset() {
      var builder = finding();

      assertThatThrownBy(builder::build).isInstanceOf(
        NullPointerException.class
      );
    }
  }
}
