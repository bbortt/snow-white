/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateReportRepository
  extends JpaRepository<@NonNull QualityGateReport, @NonNull UUID>
{
  @Modifying
  @Query(
    "UPDATE QualityGateReport r SET r.reportStatus = :status WHERE r.createdAt < :cutoff"
  )
  int updateStatusToTimedOutByCreatedAtBefore(
    @Param("cutoff") Instant cutoff,
    @Param("status") int status
  );
}
