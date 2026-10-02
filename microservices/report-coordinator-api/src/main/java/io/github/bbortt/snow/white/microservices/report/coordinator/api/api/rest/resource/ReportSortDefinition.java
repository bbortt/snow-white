/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.data.domain.Sort.Direction.DESC;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.web.SortDefinition;
import lombok.NoArgsConstructor;

/**
 * What the report listing's {@code sort} parameter accepts.
 *
 * <p>Declared here rather than in {@code commons} because the published-to-entity naming is this
 * service's own: {@code initiatedAt} is what {@code QualityGateReportMapper} serves the entity's
 * {@code createdAt} as. The entity name is not also accepted: the webapp sent it until this contract
 * landed and now sends {@code initiatedAt}, so keeping it would preserve a spelling with no caller.
 *
 * <p>{@code status} is absent on purpose. It is published as a string enum but persisted as a
 * stable numeric code whose order is neither alphabetical nor a severity ranking, so sorting by the
 * column would claim to order by the published string while ordering by an internal code.
 */
@RealizesSw({
  SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES,
  SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT,
})
@NoArgsConstructor(access = PRIVATE)
final class ReportSortDefinition {

  static final SortDefinition REPORT_SORT = SortDefinition.builder()
    .sortable("initiatedAt", "createdAt")
    .sortable("qualityGateConfigName")
    .sortable("calculationId")
    .defaultOrder(DESC, "initiatedAt")
    .tiebreaker("calculationId")
    .build();
}
