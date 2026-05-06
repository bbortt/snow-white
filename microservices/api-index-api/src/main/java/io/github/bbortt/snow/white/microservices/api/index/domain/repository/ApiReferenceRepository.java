/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.repository;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiReferenceRepository
  extends
    JpaRepository<@NonNull ApiReference, ApiReference.@NonNull ApiReferenceId>,
    JpaSpecificationExecutor<@NonNull ApiReference>
{
  boolean existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
    @Param("otelServiceName") String otelServiceName,
    @Param("apiName") String apiName,
    @Param("apiVersion") String apiVersion
  );

  boolean existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEqualsAndPrereleaseIsFalse(
    @Param("otelServiceName") String otelServiceName,
    @Param("apiName") String apiName,
    @Param("apiVersion") String apiVersion
  );

  Optional<
    ApiReference
  > findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
    @Param("otelServiceName") String otelServiceName,
    @Param("apiName") String apiName,
    @Param("apiVersion") String apiVersion
  );

  @Query(
    "SELECT DISTINCT a.otelServiceName FROM ApiReference a ORDER BY a.otelServiceName"
  )
  List<String> findDistinctServiceNames();

  @Query("SELECT DISTINCT a.apiName FROM ApiReference a ORDER BY a.apiName")
  List<String> findDistinctApiNames();

  @Query(
    "SELECT DISTINCT a.apiName FROM ApiReference a WHERE a.otelServiceName = :serviceName ORDER BY a.apiName"
  )
  List<String> findDistinctApiNamesByOtelServiceName(
    @Param("serviceName") String serviceName
  );
}
