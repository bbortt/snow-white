/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateReportRepository
  extends JpaRepository<QualityGateReport, UUID> {
  Page<QualityGateReport> findByReportStatusIn(
    @Param("reportStatus") List<ReportStatus> reportStatuses,
    Pageable pageable
  );
}
