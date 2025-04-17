/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenApiCriterionRepository
  extends JpaRepository<OpenApiCriterion, Long> {
  Optional<OpenApiCriterion> findByName(@Param("name") String name);
}
