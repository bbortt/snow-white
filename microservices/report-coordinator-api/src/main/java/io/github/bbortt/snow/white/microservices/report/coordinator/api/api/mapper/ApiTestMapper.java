/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequestIncludeApisInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
  componentModel = SPRING,
  uses = { ApiTestResultMapper.class, ReportStatusMapper.class }
)
public interface ApiTestMapper {
  default Set<ApiTest> getApiTests(
    CalculateQualityGateRequest qualityGateCalculationRequest
  ) {
    return toApiTests(qualityGateCalculationRequest.getIncludeApis());
  }

  Set<ApiTest> toApiTests(
    List<CalculateQualityGateRequestIncludeApisInner> includeApis
  );

  @Mapping(target = "id", ignore = true)
  @Mapping(
    target = "apiType",
    expression = "java(io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED.getVal())"
  )
  @Mapping(target = "apiTestResults", ignore = true)
  @Mapping(target = "qualityGateReport", ignore = true)
  @Mapping(
    target = "reportStatus",
    expression = "java(io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED.getVal())"
  )
  @Mapping(target = "stackTrace", ignore = true)
  ApiTest toApiTest(CalculateQualityGateRequestIncludeApisInner includeApi);

  @Mapping(target = "id", ignore = true)
  @Mapping(
    target = "apiType",
    expression = "java(toCommonApiType(apiDetails.getApiType()).getVal())"
  )
  @Mapping(target = "apiTestResults", ignore = true)
  @Mapping(target = "qualityGateReport", ignore = true)
  @Mapping(
    target = "reportStatus",
    expression = "java(io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED.getVal())"
  )
  @Mapping(target = "stackTrace", ignore = true)
  ApiTest toApiTest(GetAllApis200ResponseInner apiDetails);

  ApiType toCommonApiType(GetAllApis200ResponseInner.ApiTypeEnum apiType);

  @Mapping(target = "testResults", source = "apiTestResults")
  @Mapping(target = "status", source = "reportStatus")
  CalculateQualityGate202ResponseInterfacesInner toInterfaces(ApiTest apiTest);

  @Mapping(target = "testResults", source = "apiTestResults")
  @Mapping(target = "status", source = "reportStatus")
  ListQualityGateReports200ResponseInnerInterfacesInner toListInterfaces(
    ApiTest apiTest
  );
}
