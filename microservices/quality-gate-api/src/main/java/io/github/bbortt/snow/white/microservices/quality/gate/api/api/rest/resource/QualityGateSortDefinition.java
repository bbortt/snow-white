/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.data.domain.Sort.Direction.ASC;

import io.github.bbortt.snow.white.commons.web.SortDefinition;
import lombok.NoArgsConstructor;

/**
 * What the quality-gate configuration listing's {@code sort} parameter accepts.
 *
 * <p>Every published name here already matches its entity attribute, so this service needs no
 * translation — only the vocabulary, the default order and the tiebreaker.
 *
 * <p>{@code description} is absent on purpose: it is the one nullable candidate across the three
 * list reads, and admitting it would force this contract to say where a configuration without one
 * sorts. An explicit {@code NULLS} clause changes the order the existing index on {@code name}
 * provides, which is a real cost for ordering gates by prose nobody has asked to sort by.
 */
@NoArgsConstructor(access = PRIVATE)
final class QualityGateSortDefinition {

  static final SortDefinition QUALITY_GATE_SORT = SortDefinition.builder()
    .sortable("name")
    .sortable("isPredefined")
    .sortable("minCoveragePercentage")
    .defaultOrder(ASC, "name")
    .tiebreaker("name")
    .build();
}
