package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateReportRepository
  extends JpaRepository<QualityGateReport, UUID> {}
