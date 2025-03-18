package io.github.bbortt.snow.white.microservices.report.coordination.service.repository;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends CrudRepository<Report, UUID> {}
