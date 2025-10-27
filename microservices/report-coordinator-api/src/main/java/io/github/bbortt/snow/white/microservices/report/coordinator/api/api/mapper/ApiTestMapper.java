/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequestIncludeApisInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING, uses = { ApiTestResultMapper.class })
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
  @Mapping(target = "apiType", ignore = true)
  @Mapping(target = "apiTestResults", ignore = true)
  @Mapping(target = "qualityGateReport", ignore = true)
  ApiTest toApiTest(CalculateQualityGateRequestIncludeApisInner includeApi);

  @Mapping(target = "testResults", source = "apiTestResults")
  CalculateQualityGate202ResponseInterfacesInner toInterfaces(ApiTest apiTest);

  @Mapping(target = "testResults", source = "apiTestResults")
  ListQualityGateReports200ResponseInnerInterfacesInner toListInterfaces(
    ApiTest apiTest
  );
}
