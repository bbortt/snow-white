/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiTestResultMapperTest {

  private OpenApiTestResultMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiTestResultMapperImpl();
  }

  @Nested
  class OpenApiCriteriaToIncludedInReport {

    @Test
    void shouldAlwaysReturnTrue() {
      assertThat(fixture.openApiCriteriaToIncludedInReport(null))
        .isNotNull()
        .isTrue();
    }
  }
}
