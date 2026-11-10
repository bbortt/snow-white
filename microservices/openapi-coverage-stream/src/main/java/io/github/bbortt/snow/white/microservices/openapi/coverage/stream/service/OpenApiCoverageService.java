/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTERS;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTER_BYTES;
import static io.github.bbortt.snow.white.commons.event.dto.FindingEvidence.MAX_TEST_CASE_NAME_BYTES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.HEAD;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.OPTIONS;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PATCH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ConformanceVerdictReader;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.NoApplicationTelemetryException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageService {

  private static final Set<PathItemMapping> METHOD_ACCESSORS = Set.of(
    new PathItemMapping(PathItem::getGet, GET),
    new PathItemMapping(PathItem::getPost, POST),
    new PathItemMapping(PathItem::getPut, PUT),
    new PathItemMapping(PathItem::getDelete, DELETE),
    new PathItemMapping(PathItem::getPatch, PATCH),
    new PathItemMapping(PathItem::getHead, HEAD),
    new PathItemMapping(PathItem::getOptions, OPTIONS)
  );

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;
  private final OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinator;

  @WithSpan
  public Set<OpenApiTestResult> calculateCoverage(
    @NonNull OpenApiTestContext openApiTestContext
  ) throws UnmatchedWaiverException, NoApplicationTelemetryException {
    if (isEmpty(openApiTestContext.openTelemetryData())) {
      return emptySet();
    }

    var results = calculateCoverage(
      openApiTestContext.apiInformation(),
      openApiTestContext.openAPI(),
      openApiTestContext.openTelemetryData(),
      openApiTestContext.waiverContext()
    );

    // After the criteria rather than between them: a waiver is placed by the API test as a whole,
    // so the results are not handed back until every dispatched one is known to have placed
    // itself.
    WaiverMatchAccounting.verifyEveryWaiverMatched(
      openApiTestContext.apiInformation(),
      results,
      openApiTestContext.waiverContext()
    );

    return results;
  }

  private Set<OpenApiTestResult> calculateCoverage(
    ApiInformation apiInformation,
    OpenAPI openApi,
    Set<OpenTelemetryData> openTelemetryData,
    WaiverContext waiverContext
  ) throws NoApplicationTelemetryException {
    logger.info(
      "Calculating OpenAPI coverage for {} telemetry data points",
      openTelemetryData.size()
    );

    var pathIndex = buildPathIndex(openApi);
    var pathToTelemetryMap = groupTelemetryByPath(
      openTelemetryData,
      pathIndex.operationIdToOperationKey()
    );

    // Asked here and nowhere earlier: a verdict is hoisted onto its span by the grouping above, so
    // a span read straight out of a backend carries none and no caller upstream of this line can
    // tell a harness's span from the application's.
    if (onlyHarnessSpansIn(pathToTelemetryMap)) {
      throw new NoApplicationTelemetryException(apiInformation);
    }

    return openApiCoverageCalculationCoordinator.calculate(
      pathIndex.operationKeyToOperation(),
      pathToTelemetryMap,
      waiverContext
    );
  }

  /**
   * Whether a harness exported every span of this window, and the application under test none of
   * them.
   */
  @RealizesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  private static boolean onlyHarnessSpansIn(
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var spans = pathToTelemetryMap
      .values()
      .stream()
      .flatMap(List::stream)
      .toList();

    return (
      !spans.isEmpty() &&
      spans.stream().allMatch(OpenTelemetryData::isHarnessSpan)
    );
  }

  private OpenApiPathIndex buildPathIndex(OpenAPI openApi) {
    Map<String, Operation> operationKeyToOperation = new HashMap<>();
    Map<String, String> operationIdToOperationKey = new HashMap<>();

    if (isEmpty(openApi.getPaths())) {
      return new OpenApiPathIndex(
        operationKeyToOperation,
        operationIdToOperationKey
      );
    }

    openApi.getPaths().forEach((path, pathItem) -> {
      for (var pathItemMapping : METHOD_ACCESSORS) {
        var operation = pathItemMapping.mappingFunction().apply(pathItem);
        if (nonNull(operation)) {
          var operationKey = toOperationKey(
            path,
            pathItemMapping.httpMethodString()
          );
          operationKeyToOperation.put(operationKey, operation);
          if (hasText(operation.getOperationId())) {
            operationIdToOperationKey.put(
              operation.getOperationId(),
              operationKey
            );
          }
        }
      }
    });

    return new OpenApiPathIndex(
      operationKeyToOperation,
      operationIdToOperationKey
    );
  }

  /**
   * Groups the telemetry by the operation it belongs to, and hoists each span's test identity on
   * the way past.
   * <p>
   * This is the one place that knows which attributes the operator configured for a test identity
   * and for a conformance verdict, so it is the one place that reads them: a calculator is handed
   * spans that already name their test and already carry whatever verdict they hold, and stays free
   * of configuration it would only pass on. Hoisting here also keeps both out of the grouping key -
   * neither attribute influences which operation a span is attributed to.
   */
  @RealizesSw({
    SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN,
    SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES,
  })
  private Map<String, List<OpenTelemetryData>> groupTelemetryByPath(
    Set<OpenTelemetryData> telemetryData,
    Map<String, String> operationIdToOperationKey
  ) {
    var operationIdAttr =
      openApiCoverageStreamProperties.getOperationIdAttribute();
    var testCaseNameAttr =
      openApiCoverageStreamProperties.getTestCaseNameAttribute();

    // One pathological test name is carried by every span of that case, so warning per span would
    // bury the calculation's own log under thousands of identical lines. Collected here and
    // reported once instead - the sequential stream below is the only writer.
    List<String> spansWithUnstorableTestIdentity = new ArrayList<>();

    // Same reasoning, and the same shape: one misconfigured producer emits the same unreadable
    // verdict on every span it exports.
    var verdictWarnings = new ConformanceVerdictReader.Warnings();

    var pathToTelemetryMap = telemetryData
      .stream()
      .filter(data ->
        isRoutable(data, operationIdAttr, operationIdToOperationKey)
      )
      .collect(
        groupingBy(
          data ->
            resolveOperationKey(
              data,
              operationIdAttr,
              operationIdToOperationKey
            ),
          mapping(
            data ->
              hoistTestIdentity(
                data,
                testCaseNameAttr,
                spansWithUnstorableTestIdentity
              ).withConformanceVerdictFrom(
                openApiCoverageStreamProperties.getResponseSchemaValidAttribute(),
                openApiCoverageStreamProperties.getResponseSchemaViolationsAttribute(),
                verdictWarnings
              ),
            toList()
          )
        )
      );

    warnAboutUnstorableTestIdentities(
      spansWithUnstorableTestIdentity,
      testCaseNameAttr
    );

    warnAboutUnreadableVerdicts(verdictWarnings);

    return pathToTelemetryMap;
  }

  /**
   * What the verdict reading had to decide for a producer rather than read from it, reported once
   * per calculation.
   */
  @RealizesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
  private void warnAboutUnreadableVerdicts(
    ConformanceVerdictReader.Warnings warnings
  ) {
    var unreadable = warnings.spansWithUnreadableVerdict();

    if (!unreadable.isEmpty()) {
      // Loud rather than silent: these responses are reported as breaking their schema on a
      // verdict their validator did not state readably - at times without having said so at all -
      // and only the producer's own configuration explains it. The values are not logged - they
      // are the untrusted part.
      logger.warn(
        "Read the conformance verdict of {} span(s) as non-conforming, '{}' among them: attribute '{}' was absent where violations were reported, held no boolean, or stated true alongside violations - or attribute '{}' was not a JSON array of strings, and so stated no violation list to read the verdict against. A verdict Snow-White cannot read must not pass.",
        unreadable.size(),
        unreadable.getFirst(),
        openApiCoverageStreamProperties.getResponseSchemaValidAttribute(),
        openApiCoverageStreamProperties.getResponseSchemaViolationsAttribute()
      );
    }

    var unusablePointers = warnings.spansWithUnusablePointers();

    if (!unusablePointers.isEmpty()) {
      logger.warn(
        "Dropped violation pointers of {} span(s), '{}' among them: attribute '{}' named more than the {} pointers of {} UTF-8 bytes a verdict carries. Their verdicts are unaffected - a dropped pointer costs a finding's detail, never the finding.",
        unusablePointers.size(),
        unusablePointers.getFirst(),
        openApiCoverageStreamProperties.getResponseSchemaViolationsAttribute(),
        MAX_VIOLATION_POINTERS,
        MAX_VIOLATION_POINTER_BYTES
      );
    }
  }

  private OpenTelemetryData hoistTestIdentity(
    OpenTelemetryData data,
    String testCaseNameAttr,
    List<String> spansWithUnstorableTestIdentity
  ) {
    var withTestIdentity = data.withTestIdentityFrom(testCaseNameAttr);

    // A span that named a test yet holds no identity afterwards named one too long to store: the
    // only reason the hoist discards a value it was given.
    if (
      isNull(withTestIdentity.testCaseName()) &&
      data.namesATestIn(testCaseNameAttr)
    ) {
      spansWithUnstorableTestIdentity.add(data.spanId());
    }

    return withTestIdentity;
  }

  private void warnAboutUnstorableTestIdentities(
    List<String> spanIds,
    String testCaseNameAttr
  ) {
    if (spanIds.isEmpty()) {
      return;
    }

    // Loud rather than silent: from the outside a dropped identity and an absent one look
    // identical, and an operator debugging "why is every finding unnamed?" has no other way to
    // tell them apart. The names themselves are not logged - they are the thing that is too long.
    logger.warn(
      "Dropped the test identity of {} span(s), '{}' among them: attribute '{}' held more than the {} UTF-8 bytes Snow-White stores. Their evidence keeps the trace id, but names no test.",
      spanIds.size(),
      spanIds.getFirst(),
      testCaseNameAttr,
      MAX_TEST_CASE_NAME_BYTES
    );
  }

  private boolean isRoutable(
    OpenTelemetryData data,
    String operationIdAttr,
    Map<String, String> operationIdToOperationKey
  ) {
    if (data.attributes().has(operationIdAttr)) {
      var operationId = data.attributes().get(operationIdAttr).asString();
      if (
        hasText(operationId) &&
        operationIdToOperationKey.containsKey(operationId)
      ) {
        return true;
      }
    }
    // TODO: This should also take request/response into account!
    return (
      data.attributes().has(HTTP_REQUEST_METHOD.getKey()) &&
      data.attributes().has(URL_PATH.getKey())
    );
  }

  private String resolveOperationKey(
    OpenTelemetryData data,
    String operationIdAttr,
    Map<String, String> operationIdToOperationKey
  ) {
    if (data.attributes().has(operationIdAttr)) {
      var operationId = data.attributes().get(operationIdAttr).asString();
      if (hasText(operationId)) {
        var resolvedKey = operationIdToOperationKey.get(operationId);
        if (nonNull(resolvedKey)) {
          return resolvedKey;
        }
      }
    }

    return toOperationKey(
      data.attributes().get(URL_PATH.getKey()).asString(),
      data.attributes().get(HTTP_REQUEST_METHOD.getKey()).asString()
    );
  }

  private record PathItemMapping(
    Function<PathItem, Operation> mappingFunction,
    PathItem.HttpMethod httpMethod
  ) {
    public String httpMethodString() {
      return httpMethod.name();
    }
  }

  private record OpenApiPathIndex(
    Map<String, Operation> operationKeyToOperation,
    Map<String, String> operationIdToOperationKey
  ) {}
}
