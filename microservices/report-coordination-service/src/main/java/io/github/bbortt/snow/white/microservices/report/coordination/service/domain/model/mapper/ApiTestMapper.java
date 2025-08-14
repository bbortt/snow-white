/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING, uses = { ApiTestResultMapper.class })
public interface ApiTestMapper {
  @Mapping(target = "testResults", source = "apiTestResults")
  CalculateQualityGate202ResponseInterfacesInner toDto(ApiTest apiTest);

  @Mapping(target = "testResults", source = "apiTestResults")
  ListQualityGateReports200ResponseInnerInterfacesInner toListDto(
    ApiTest apiTest
  );

  default CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum toApiTypeEnum(
    Integer apiType
  ) {
    return switch (apiType) {
      case 1 -> CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum.ASYNCAPI;
      case 2 -> CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum.OPENAPI;
      case 3 -> CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum.GRAPHQL;
      default -> throw new IllegalArgumentException(
        "Unknown API type: " + apiType
      );
    };
  }

  default ListQualityGateReports200ResponseInnerInterfacesInner.ApiTypeEnum toListApiTypeEnum(
    Integer apiType
  ) {
    return switch (apiType) {
      case 1 -> ListQualityGateReports200ResponseInnerInterfacesInner.ApiTypeEnum.ASYNCAPI;
      case 2 -> ListQualityGateReports200ResponseInnerInterfacesInner.ApiTypeEnum.OPENAPI;
      case 3 -> ListQualityGateReports200ResponseInnerInterfacesInner.ApiTypeEnum.GRAPHQL;
      default -> throw new IllegalArgumentException(
        "Unknown API type: " + apiType
      );
    };
  }
}
