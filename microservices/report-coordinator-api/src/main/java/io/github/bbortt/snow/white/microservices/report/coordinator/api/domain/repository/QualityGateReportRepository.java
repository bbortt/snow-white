/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateReportRepository
  extends
    JpaRepository<@NonNull QualityGateReport, @NonNull UUID>,
    JpaSpecificationExecutor<@NonNull QualityGateReport>
{
  /**
   * The single-report read, which serves findings alongside the coverage they explain. Both
   * {@code findings} and {@code evidence} are lazy by default - see
   * {@link io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding} -
   * so the list-shaped reads never drag the grandchild table in; this fetch plan is what makes them
   * present, rather than merely declared, on the one response that promises them.
   * <p>
   * {@code LOAD}, not the default {@code FETCH}: a {@code FETCH} graph treats every attribute it
   * does not name as lazy, which would demote the eagerly declared {@code reportParameter} and
   * {@code apiTests} to proxies and strand the mapping outside the session.
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @EntityGraph(
    attributePaths = { "apiTests.apiTestResults.findings.evidence" },
    type = LOAD
  )
  Optional<QualityGateReport> findWithFindingsByCalculationId(
    UUID calculationId
  );

  /**
   * The stored status code and nothing else, for the read that has to know which shape a report
   * gets before it fetches the report. {@code findById} would answer the same question, but
   * {@code reportParameter}, {@code apiTests} and their results are declared eager, so asking it for
   * one column materializes the whole graph - twice, once for the decision and once for the body.
   * <p>
   * The projection costs the in-progress poll one extra primary-key lookup on an indexed column,
   * which is the side of the trade that loses least: the poll reads the narrow report either way,
   * while the completed read - the drilldown this endpoint exists for - stops loading the graph it
   * is about to load again with its findings.
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @Query(
    "SELECT r.reportStatus FROM QualityGateReport r WHERE r.calculationId = :calculationId"
  )
  Optional<Short> findReportStatusByCalculationId(
    @Param("calculationId") UUID calculationId
  );

  @RealizesSw(SwTraceables.SW_018_STALE_REPORTS_TIME_OUT_NOT_DELETED)
  @Modifying
  @Query(
    "UPDATE QualityGateReport r SET r.reportStatus = :status WHERE r.createdAt < :cutoff AND r.reportStatus IN (:initialStatus)"
  )
  int updateStatusToTimedOutByCreatedAtBefore(
    @Param("cutoff") Instant cutoff,
    @Param("status") int status,
    @Param("initialStatus") Set<Short> initialStatus
  );
}
