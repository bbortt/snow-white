/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.api.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.mapper.QualityGateConfigMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith({ MockitoExtension.class })
class QualityGateServiceTest {

  private static final String TEST_QUALITY_GATE_NAME = "test-quality-gate";

  @Mock
  private QualityGateApi qualityGateApiMock;

  @Mock
  private QualityGateConfigMapper qualityGateConfigMapperMock;

  @Mock
  private io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig qualityGateDto;

  @Mock
  private QualityGateConfig qualityGateConfig;

  private QualityGateService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateService(
      qualityGateApiMock,
      qualityGateConfigMapperMock
    );
  }

  @Nested
  class findQualityGateConfigByName {

    @Test
    void shouldReturnConfigWhenFound() throws QualityGateNotFoundException {
      doReturn(qualityGateDto)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);
      doReturn(qualityGateConfig)
        .when(qualityGateConfigMapperMock)
        .fromDto(qualityGateDto);

      QualityGateConfig result = fixture.findQualityGateConfigByName(
        TEST_QUALITY_GATE_NAME
      );

      assertThat(result).isEqualTo(qualityGateConfig);
    }

    @Test
    void shouldThrow_whenQualityGateIsNotPresent() {
      doReturn(null)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).isInstanceOf(QualityGateNotFoundException.class);
    }

    @Test
    void shouldThrow_whenQueryThrows() {
      var notFoundExceptionMock = mock(RestClientResponseException.class);

      doReturn(NOT_FOUND).when(notFoundExceptionMock).getStatusCode();
      doThrow(notFoundExceptionMock)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).isInstanceOf(QualityGateNotFoundException.class);
    }

    @Test
    void shouldPropagateOtherExceptions() {
      var otherException = mock(RestClientResponseException.class);

      doReturn(INTERNAL_SERVER_ERROR).when(otherException).getStatusCode();
      doThrow(otherException)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).isSameAs(otherException);
    }
  }
}
