/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApiTestMapperTest {

  @Nested
  class ToApiTypeEnum {

    @ParameterizedTest
    @EnumSource(ApiType.class)
    void shouldMapValuesToApiTypeEnum(ApiType apiType) {
      if (UNSPECIFIED.equals(apiType)) {
        return;
      }

      assertThat(
        new ApiTestMapperImpl().toApiTypeEnum(apiType.getVal())
      ).isEqualTo(
        CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum.valueOf(
          apiType.name()
        )
      );
    }

    @Test
    void shouldThrowExceptionWhenApiTypeIsUnspecified() {
      assertThatThrownBy(() ->
        new ApiTestMapperImpl().toApiTypeEnum(UNSPECIFIED.getVal())
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown API type: " + UNSPECIFIED.getVal());
    }
  }

  @Nested
  class ToListApiTypeEnum {

    @ParameterizedTest
    @EnumSource(ApiType.class)
    void shouldMapValuesToApiTypeEnum(ApiType apiType) {
      if (UNSPECIFIED.equals(apiType)) {
        return;
      }

      assertThat(
        new ApiTestMapperImpl().toListApiTypeEnum(apiType.getVal())
      ).isEqualTo(
        ListQualityGateReports200ResponseInnerInterfacesInner.ApiTypeEnum.valueOf(
          apiType.name()
        )
      );
    }

    @Test
    void shouldThrowExceptionWhenApiTypeIsUnspecified() {
      assertThatThrownBy(() ->
        new ApiTestMapperImpl().toListApiTypeEnum(UNSPECIFIED.getVal())
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown API type: " + UNSPECIFIED.getVal());
    }
  }
}
