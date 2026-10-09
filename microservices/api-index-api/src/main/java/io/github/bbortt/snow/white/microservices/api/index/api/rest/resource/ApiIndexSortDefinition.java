/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest.resource;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.data.domain.Sort.Direction.ASC;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.web.SortDefinition;
import lombok.NoArgsConstructor;

/**
 * What the ingested-APIs listing's {@code sort} parameter accepts.
 *
 * <p>{@code serviceName} is what {@code ApiReferenceMapper} serves the entity's
 * {@code otelServiceName} as, so the published name is the one declared here. The entity name is not
 * also accepted: the webapp sent it until this contract landed and now sends {@code serviceName}, so
 * keeping it would preserve a spelling with no caller.
 *
 * <p>The default order is the primary key's own column order, so ordering by default needs no
 * index this table does not already have. {@code indexedAt} is not a candidate: the entity carries
 * it, but {@code ApiInformation.yml} does not publish it, and only published names are sortable.
 *
 * <p>{@code apiVersion} orders as a string, so {@code 1.10.0} precedes {@code 1.9.0}. The column
 * has no parsed form, and a correct version order is a feature of its own rather than a property of
 * this sort contract.
 */
@RealizesSw({
  SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES,
  SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT,
})
@NoArgsConstructor(access = PRIVATE)
final class ApiIndexSortDefinition {

  static final SortDefinition API_INDEX_SORT = SortDefinition.builder()
    .sortable("serviceName", "otelServiceName")
    .sortable("apiName")
    .sortable("apiVersion")
    .sortable("apiType")
    .defaultOrder(ASC, "serviceName")
    .tiebreaker("serviceName", "apiName", "apiVersion")
    .build();
}
