/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateConfigurationRepository
  extends JpaRepository<@NonNull QualityGateConfiguration, @NonNull Long>
{
  boolean existsByName(@Param("name") String name);
  Optional<QualityGateConfiguration> findByName(@Param("name") String name);
  void deleteByName(@Param("name") String name);

  /**
   * Takes the lock the startup gate-file apply holds, released when that transaction ends. In
   * high-available mode three replicas start together and each one replaces the whole gate table,
   * so without this two of them delete and re-insert the same names at once and collide on the
   * unique name constraint — a pod that crash-loops until the startups happen not to overlap. The
   * losers wait here instead, then apply the same file to the same result.
   */
  @Query(value = "SELECT pg_advisory_xact_lock(4208862275)", nativeQuery = true)
  Object lockGateFileApply();
}
