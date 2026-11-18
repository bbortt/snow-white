/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding.MAX_EVIDENCE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
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

    /**
     * The canonical constructor deliberately does not bound: an event serialized before
     * {@link ApiTestFinding#MAX_EVIDENCE} existed has to deserialize whole.
     */
    @Test
    void shouldNotBeBoundedOnConstruction() {
      var evidence = evidenceOfOneTestCase("suite", MAX_EVIDENCE + 10);

      var result = finding().evidence(evidence).build();

      assertThat(result.evidence()).hasSize(MAX_EVIDENCE + 10);
    }
  }

  @Nested
  class BoundedTest {

    @Test
    void shouldReturnEvidenceBelowTheBoundUnchanged() {
      var evidence = twoTestCasesGrouped(MAX_EVIDENCE - 1);

      assertThat(ApiTestFinding.bounded(evidence)).containsExactlyElementsOf(
        evidence
      );
    }

    /**
     * At exactly the bound nothing is dropped and nothing is reordered. The evidence is grouped by
     * test so that the diversity-first pass would visibly reorder it, which is what makes this an
     * assertion about the boundary rather than about a list that happens to survive either path.
     */
    @Test
    void shouldReturnEvidenceAtTheBoundUnchanged() {
      var evidence = twoTestCasesGrouped(MAX_EVIDENCE);

      assertThat(ApiTestFinding.bounded(evidence)).containsExactlyElementsOf(
        evidence
      );
    }

    @Test
    void shouldDropEvidencePastTheBound() {
      var evidence = evidenceOfOneTestCase("suite", MAX_EVIDENCE + 1);

      assertThat(ApiTestFinding.bounded(evidence)).hasSize(MAX_EVIDENCE);
    }

    @Test
    void shouldBeUnmodifiable() {
      var bounded = ApiTestFinding.bounded(
        evidenceOfOneTestCase("suite", MAX_EVIDENCE + 1)
      );
      var otherEvidence = new FindingEvidence("otherTraceId", null);

      assertThatThrownBy(() -> bounded.add(otherEvidence)).isInstanceOf(
        UnsupportedOperationException.class
      );
    }

    /**
     * The failure a blind truncation would cause: the spans attributed to a target arrive grouped
     * by test, so taking the first {@code MAX_EVIDENCE} entries keeps repeat traces of the earliest
     * tests and loses every later one. Every test that reached the target has to survive while
     * there is room for it.
     */
    @Test
    void shouldKeepEveryTestIdentityRatherThanTheFirstTracesOnly() {
      List<FindingEvidence> evidence = new ArrayList<>();
      for (var testCase = 0; testCase < MAX_EVIDENCE; testCase++) {
        evidence.addAll(evidenceOfOneTestCase("suite-" + testCase, 10));
      }

      var result = ApiTestFinding.bounded(evidence);

      assertThat(result)
        .hasSize(MAX_EVIDENCE)
        .extracting(FindingEvidence::testCaseName)
        .doesNotHaveDuplicates()
        .hasSize(MAX_EVIDENCE);
    }

    /**
     * More tests than there is room for. Which ones survive has to be the first
     * {@code MAX_EVIDENCE} in arrival order and nothing else: a reader comparing two reports, or a
     * report regenerated from the same telemetry, cannot have the identities shift underneath it.
     */
    @Test
    void shouldKeepTheFirstIdentitiesInOrderWhenThereAreMoreThanRoom() {
      List<FindingEvidence> evidence = new ArrayList<>();
      for (var testCase = 0; testCase < MAX_EVIDENCE * 3; testCase++) {
        evidence.addAll(evidenceOfOneTestCase("suite-" + testCase, 2));
      }

      var result = ApiTestFinding.bounded(evidence);

      assertThat(result)
        .hasSize(MAX_EVIDENCE)
        .containsExactlyElementsOf(
          IntStream.range(0, MAX_EVIDENCE)
            .mapToObj(testCase ->
              new FindingEvidence(
                "suite-" + testCase + "-0",
                "suite-" + testCase
              )
            )
            .toList()
        );
    }

    /**
     * Spans carrying no test identity reduce to their trace ids, so the null name is one identity
     * among the others rather than a reason to drop them.
     */
    @Test
    void shouldTreatMissingTestIdentityAsOneIdentity() {
      List<FindingEvidence> evidence = new ArrayList<>(
        evidenceOfOneTestCase(null, MAX_EVIDENCE)
      );
      evidence.add(new FindingEvidence("namedTrace", "suite"));

      var result = ApiTestFinding.bounded(evidence);

      assertThat(result)
        .hasSize(MAX_EVIDENCE)
        .contains(new FindingEvidence("namedTrace", "suite"));
    }

    /**
     * Once every identity is represented the remaining room goes to further traces, in the order
     * they were given - a target covered by one test still carries proof it ran repeatedly.
     */
    @Test
    void shouldFillRemainingRoomWithFurtherTracesInOrder() {
      List<FindingEvidence> evidence = new ArrayList<>();
      evidence.add(new FindingEvidence("trace-a", "suite-a"));
      evidence.addAll(evidenceOfOneTestCase("suite-b", MAX_EVIDENCE + 5));

      var result = ApiTestFinding.bounded(evidence);

      assertThat(result)
        .hasSize(MAX_EVIDENCE)
        .startsWith(
          new FindingEvidence("trace-a", "suite-a"),
          new FindingEvidence("suite-b-0", "suite-b")
        );
    }
  }

  /**
   * All of one test's traces, then all of another's - the shape real telemetry arrives in, and the
   * shape the diversity-first pass reorders.
   */
  private static List<FindingEvidence> twoTestCasesGrouped(int count) {
    List<FindingEvidence> evidence = new ArrayList<>(
      evidenceOfOneTestCase("suite-a", count / 2)
    );
    evidence.addAll(evidenceOfOneTestCase("suite-b", count - count / 2));
    return evidence;
  }

  private static List<FindingEvidence> evidenceOfOneTestCase(
    @Nullable String testCaseName,
    int count
  ) {
    return IntStream.range(0, count)
      .mapToObj(trace ->
        new FindingEvidence(testCaseName + "-" + trace, testCaseName)
      )
      .toList();
  }
}
