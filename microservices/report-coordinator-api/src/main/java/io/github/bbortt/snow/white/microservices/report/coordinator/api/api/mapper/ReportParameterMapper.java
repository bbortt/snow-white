/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportParameterMapper {
  /**
   * A body that writes {@code "waivers": null} rather than omitting the field named no waivers,
   * which is the same document as an absent field - so it maps to the empty list the entity
   * defaults to.
   * <p>
   * The default is written out because the generated element mapper answers a null list with a
   * null list, and passing that to the builder <em>overrides</em> {@code @Builder.Default} rather
   * than falling back to it. The trigger's own waiver validation would then be the first thing to
   * dereference it, turning a request it should have accepted into a {@code 500}.
   */
  @Mapping(target = "calculationId", source = "calculationId")
  @Mapping(target = "lookbackWindow", defaultValue = "1h")
  @Mapping(
    target = "waivers",
    defaultExpression = "java(new java.util.ArrayList<>())"
  )
  @Mapping(target = "qualityGateReport", ignore = true)
  ReportParameter fromDto(
    CalculateQualityGateRequest qualityGateCalculationRequest,
    UUID calculationId
  );
}
