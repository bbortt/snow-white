/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_SCHEMA_CONFORMANCE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.PATHS_POINTER;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponseEntryPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ViolationPointerResolver.resolve;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each response a test harness judged against its documented schema matched that schema.
 * <p>
 * The first criterion that judges what an API answered rather than whether it was reached, and the
 * only one that reads the spans a test harness exported. It recomputes nothing: the verdict was
 * reached by the validator the test already ran, and this calculator reports it.
 * <p>
 * The target unit is the one {@code RESPONSE_CODE_COVERAGE} enumerates - one documented response
 * entry of one documented operation - which is why this extends it rather than restating how an
 * observed status code is matched to a documented entry, {@code default} included. What differs is
 * the question asked of the spans that entry collected: not whether there are any, but what they
 * vouched for.
 *
 * @see OpenApiCoverageCriteria#RESPONSE_SCHEMA_CONFORMANCE
 */
@Component
@RealizesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
public class ResponseSchemaConformanceCalculator
  extends ResponseCodeCoverageCalculator
{

  /**
   * What an operator who configured this criterion and received no judged pair needs to read,
   * because from the outside a conforming API and a producer that never published look identical.
   * <p>
   * It says that nothing <em>matched</em> rather than that nothing was found, because the two have
   * the same symptom and different remedies: a verdict naming a response code the document does not
   * declare satisfies no documented pair, so this text is emitted while verdicts exist, and
   * "none was found" would send a reader looking for a producer that is already running.
   */
  static final String NO_VERDICT_INFORMATION =
    "No response schema conformance verdict matched a documented response of this API. Either no test harness published a verdict, or the verdicts published matched no response this specification declares - whether the status code or the operation is the undeclared one, the undocumented-response-code findings name that traffic. Responses are judged only where a test harness reports a verdict: a Citrus suite does so through `citrus-junit-jupiter-extension`, and any other framework can through the producer contract documented in `semantic-convention/openapi.md`.";

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return RESPONSE_SCHEMA_CONFORMANCE;
  }

  /**
   * The one calculator a harness's spans are handed to, and the reason they are admitted to the
   * analysed dataset at all.
   */
  @Override
  @RealizesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  protected boolean readsHarnessSpans() {
    return true;
  }

  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var findings = pathToOpenAPIOperationMap
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .flatMap(entry ->
        toOperationFindings(
          entry.getKey(),
          entry.getValue(),
          pathToTelemetryMap
        ).stream()
      )
      .toList();

    if (findings.isEmpty() || judgedAnyPair(findings)) {
      return findings;
    }

    // Every pair inapplicable would derive a vacuous 1.00 - a green pass for a check that never
    // ran. One judged target at the root of this criterion's own target space turns that into the
    // 0 over 1 it is, through the one derivation rather than beside it.
    var withNoVerdictFinding = new ArrayList<>(findings);
    withNoVerdictFinding.add(
      ApiTestFinding.builder()
        .specPointer(PATHS_POINTER)
        .status(UNCOVERED)
        .evidence(emptyList())
        .build()
    );

    return withNoVerdictFinding;
  }

  /**
   * One finding per documented response entry of the operation that carries a verdict, plus one per
   * violation where a verdict found any - and a {@code NOT_APPLICABLE} finding for every entry no
   * harness judged, which is what keeps the list an enumeration of the target space rather than of
   * the responses that happened to be reported.
   */
  @RealizesSw({
    SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE,
    SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION,
  })
  private @NonNull List<ApiTestFinding> toOperationFindings(
    @NonNull String operationKey,
    @NonNull Operation operation,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var documentedResponseCodes = extractResponseCodes(operation);
    var observedResponseCodes = extractObservedResponseCodes(
      getTelemetryForTemplate(pathToTelemetryMap, operationKey)
    );

    List<ApiTestFinding> findings = new ArrayList<>();

    for (var responseCode : documentedResponseCodes
      .stream()
      .sorted(comparing(ResponseCode::errorCode))
      .toList()) {
      findings.addAll(
        toResponseEntryFindings(
          operationKey,
          responseCode,
          documentedResponseCodes,
          observedResponseCodes,
          response(operation, responseCode.errorCode())
        )
      );
    }

    return findings;
  }

  private @NonNull List<ApiTestFinding> toResponseEntryFindings(
    @NonNull String operationKey,
    @NonNull ResponseCode responseCode,
    @NonNull List<ResponseCode> documentedResponseCodes,
    @NonNull List<ObservedResponseCode> observedResponseCodes,
    @Nullable ApiResponse response
  ) {
    var satisfyingTelemetry = getSatisfyingTelemetry(
      responseCode,
      documentedResponseCodes,
      observedResponseCodes
    );

    var judgedSpans = satisfyingTelemetry
      .stream()
      .filter(OpenTelemetryData::isHarnessSpan)
      .toList();

    if (judgedSpans.isEmpty()) {
      return List.of(
        finding(operationKey, responseCode, NOT_APPLICABLE)
          .evidence(emptyList())
          .build()
      );
    }

    var violatingSpans = judgedSpans
      .stream()
      .filter(ResponseSchemaConformanceCalculator::violated)
      .toList();

    if (violatingSpans.isEmpty()) {
      // The judged spans only, here as in the uncovered case below: a span no harness judged is
      // not evidence that a response conformed to its schema, whatever else it evidences.
      return List.of(
        finding(operationKey, responseCode, COVERED)
          .evidence(toEvidence(judgedSpans))
          .build()
      );
    }

    // One violation anywhere in the pair's telemetry leaves the pair uncovered, and each distinct
    // pointer is its own finding: a schema the API breaks sometimes is a schema it does not keep,
    // and two violations on one pair are two things to fix - or to waive - rather than one.
    // Grouped after the pointers resolve, not before: several pointers into a schema the document
    // only $refs resolve to the same ancestor, and the same finding reported twice would raise the
    // pair's judged count by two.
    Map<String, Set<OpenTelemetryData>> spansByResolvedPointer =
      new LinkedHashMap<>();

    // Resolving walks every media type's schema, and thousands of spans of one suite report the
    // same handful of pointers: the walk runs once per distinct pointer the pair was given.
    Map<String, String> resolvedPointers = new HashMap<>();
    var responseEntryPointer = toResponseEntryPointer(
      operationKey,
      responseCode.errorCode()
    );

    for (var span : violatingSpans) {
      for (var violationPointer : violationPointersOf(span)) {
        spansByResolvedPointer
          .computeIfAbsent(
            resolvedPointers.computeIfAbsent(violationPointer, pointer ->
              resolve(responseEntryPointer, response, pointer)
            ),
            resolvedPointer -> new LinkedHashSet<>()
          )
          .add(span);
      }
    }

    if (spansByResolvedPointer.isEmpty()) {
      return List.of(
        finding(operationKey, responseCode, UNCOVERED)
          .evidence(toEvidence(violatingSpans))
          .build()
      );
    }

    // Evidence names the spans that reported the violation, and not every span the pair collected:
    // a test whose response conformed is not evidence that the response did not, and sending a
    // reader to it is worse than sending them to nothing.
    return spansByResolvedPointer
      .entrySet()
      .stream()
      .map(entry ->
        finding(operationKey, responseCode, UNCOVERED)
          .specPointer(entry.getKey())
          .evidence(toEvidence(List.copyOf(entry.getValue())))
          .build()
      )
      .toList();
  }

  /**
   * Whether the harness judged this span's response and found it did not conform.
   */
  private static boolean violated(OpenTelemetryData span) {
    var verdict = span.conformanceVerdict();

    return nonNull(verdict) && !verdict.conforming();
  }

  /**
   * The pointers the span's verdict named, which a verdict that failed without saying where carries
   * none of.
   */
  private static List<String> violationPointersOf(OpenTelemetryData span) {
    var verdict = span.conformanceVerdict();

    return isNull(verdict) ? emptyList() : verdict.violationPointers();
  }

  private static ApiTestFinding.ApiTestFindingBuilder finding(
    String operationKey,
    ResponseCode responseCode,
    FindingStatus status
  ) {
    return ApiTestFinding.builder()
      .specPointer(
        toResponseEntryPointer(operationKey, responseCode.errorCode())
      )
      .status(status)
      .httpPath(toPath(operationKey))
      .httpMethod(toMethod(operationKey))
      .responseCode(responseCode.errorCode());
  }

  private static @Nullable ApiResponse response(
    Operation operation,
    String responseCode
  ) {
    return isNull(operation.getResponses())
      ? null
      : operation.getResponses().get(responseCode);
  }

  /**
   * Whether any pair of this API carried a verdict at all, asked of the findings as this calculator
   * emitted them - before waivers, which is the only point at which {@code NOT_APPLICABLE} still
   * means "no harness judged this" rather than "a harness judged it, or somebody waived it".
   */
  private static boolean judgedAnyPair(List<ApiTestFinding> findings) {
    return findings
      .stream()
      .anyMatch(finding -> !NOT_APPLICABLE.equals(finding.status()));
  }

  /**
   * The producer contract, where this calculation found no verdict to read, and the verdicts it
   * read and could not place, where a harness judged a response the document does not declare.
   * <p>
   * Either half may be absent, and one calculation can carry both: a harness that judged nothing
   * but undeclared responses leaves every documented pair unjudged, which is exactly the case the
   * no-verdict text alone would misdescribe.
   * <p>
   * The no-verdict half is read off the presence of the root finding rather than derived a second
   * time: by the time this is asked, waivers have rewritten the statuses the derivation looked at,
   * and a waiver on any one pair would answer "something was judged" and drop the explanation of a
   * gate still failing at 0 over 1 - the one case an operator cannot diagnose from the report alone.
   */
  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var noVerdict = calculation
      .findings()
      .stream()
      .anyMatch(finding -> PATHS_POINTER.equals(finding.specPointer()))
      ? NO_VERDICT_INFORMATION
      : null;

    return joinAdditionalInformation(
      noVerdict,
      droppedVerdictInformationOrNull(calculation)
    );
  }

  /**
   * The verdicts a harness published that this criterion had to leave uncounted, named so that the
   * drop is visible in the report rather than only in the gap between what a producer published and
   * what the ratio was taken over.
   * <p>
   * Said here rather than reported as a finding, because the drop is not a verdict about the API: a
   * response the document does not declare has no documented schema to be judged against, so there
   * is no target to mark uncovered and no node a spec pointer could address. The traffic itself is
   * the undocumented-response-code criterion's to report, and a second criterion reporting it would
   * count one gap twice. What belongs here is only that a verdict went uncounted - an operator
   * reading a conformance ratio has no other way to learn it.
   * <p>
   * Taken as what the pairs above did <em>not</em> collect, rather than re-deciding per operation
   * which observed code goes undeclared. A concrete request key matches the pattern of every
   * templated operation it could have been routed to, so one span is collected by more than one
   * operation and a per-operation walk reports it once per match - naming an operation no request
   * reached, over a count that is too high. Subtraction also needs no case of its own for a
   * documented {@code default}, which collects the span like any other entry, and it catches the
   * verdict whose span names no status code at all, which no observed-code walk can see.
   */
  @RealizesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
  private @Nullable String droppedVerdictInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var harnessSpans = calculation
      .pathToTelemetryMap()
      .values()
      .stream()
      .flatMap(List::stream)
      .filter(OpenTelemetryData::isHarnessSpan)
      .toList();

    // Asked before the walk below, because by far the commonest calculation has no harness span in
    // it at all and would otherwise pay for the whole enumeration to be told nothing was dropped.
    if (harnessSpans.isEmpty()) {
      return null;
    }

    // Identified by span id rather than by the record, whose equality walks the whole attribute
    // tree of every span it is compared against - the one cost this walk could carry into a
    // calculation holding thousands of them.
    Set<String> countedSpanIds = new HashSet<>();

    for (var entry : calculation.pathToOpenAPIOperationMap().entrySet()) {
      var documentedResponseCodes = extractResponseCodes(entry.getValue());
      var observedResponseCodes = extractObservedResponseCodes(
        getTelemetryForTemplate(
          calculation.pathToTelemetryMap(),
          entry.getKey()
        )
      );

      for (var responseCode : documentedResponseCodes) {
        getSatisfyingTelemetry(
          responseCode,
          documentedResponseCodes,
          observedResponseCodes
        )
          .stream()
          .map(OpenTelemetryData::spanId)
          .forEach(countedSpanIds::add);
      }
    }

    var uncounted = harnessSpans
      .stream()
      .filter(span -> !countedSpanIds.contains(span.spanId()))
      .toList();

    if (uncounted.isEmpty()) {
      return null;
    }

    return "A test harness published %d response schema conformance verdict(s) this criterion could not place, and none of them was counted: %s. A verdict is judged only where it names a response this specification declares, under an operation it declares; the undocumented-response-code findings report traffic that matched neither.".formatted(
      uncounted.size(),
      describeUncounted(uncounted)
    );
  }

  /**
   * Why the uncounted verdicts could not be placed, grouped by cause rather than listed per span:
   * one misconfigured producer emits the same shape on every span it exports.
   * <p>
   * The status codes are reported as what matched no documented response rather than as codes the
   * specification does not declare, because those are not the same claim: a verdict is equally
   * unplaced when its span reached no documented operation at all, and the code it names may be
   * declared under every operation but that one.
   */
  private String describeUncounted(List<OpenTelemetryData> uncounted) {
    var withStatusCode = extractObservedResponseCodes(uncounted);

    Set<String> unmatchedResponseCodes = new TreeSet<>();
    withStatusCode
      .stream()
      .map(ObservedResponseCode::statusCode)
      .forEach(unmatchedResponseCodes::add);

    List<String> causes = new ArrayList<>();

    if (!unmatchedResponseCodes.isEmpty()) {
      causes.add(
        "%d that matched no documented response (status code(s) %s)".formatted(
          withStatusCode.size(),
          String.join(", ", unmatchedResponseCodes)
        )
      );
    }

    var withoutStatusCode = uncounted.size() - withStatusCode.size();

    if (withoutStatusCode > 0) {
      causes.add(
        "%d on spans carrying no readable HTTP status code".formatted(
          withoutStatusCode
        )
      );
    }

    return String.join("; ", causes);
  }
}
