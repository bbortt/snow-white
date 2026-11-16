/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.api.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.mapper.QualityGateConfigMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith({ MockitoExtension.class })
class QualityGateServiceUnitTest {

  private static final String TEST_QUALITY_GATE_NAME = "test-quality-gate";

  private static final String OWN_MESSAGE =
    "No Quality-Gate configuration with ID 'test-quality-gate' exists!";

  private static final String OWNER_MESSAGE =
    "No Quality-Gate configuration with name 'test-quality-gate' exists! This instance's Quality-Gates are defined by its gate file, and that file defines none: add the Quality-Gate to the file and restart this instance!";

  @Mock
  private QualityGateApi qualityGateApiMock;

  @Mock
  private QualityGateConfigMapper qualityGateConfigMapperMock;

  @Mock
  private io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto.QualityGateConfig qualityGateDto;

  @Mock
  private QualityGateConfig qualityGateConfig;

  @InjectMocks
  private QualityGateService fixture;

  @Nested
  class findQualityGateConfigByNameTest {

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
    @VerifiesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
    void shouldSurfaceTheOwnersRejection_whenItCarriesOne() {
      notFoundRejectionCarrying(
        new Error().code("Not Found").message(OWNER_MESSAGE)
      );

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      )
        .isInstanceOf(QualityGateNotFoundException.class)
        .hasMessage(OWNER_MESSAGE);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
    void shouldKeepItsOwnWording_whenTheRejectionCarriesNoBody() {
      notFoundRejectionCarrying(null);

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).hasMessage(OWN_MESSAGE);
    }

    @Test
    void shouldKeepItsOwnWording_whenTheOwnersMessageIsBlank() {
      notFoundRejectionCarrying(new Error().code("Not Found").message(" "));

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).hasMessage(OWN_MESSAGE);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
    void shouldKeepItsOwnWording_whenTheBodyCannotBeRead() {
      var notFoundExceptionMock = mock(RestClientResponseException.class);

      doReturn(NOT_FOUND).when(notFoundExceptionMock).getStatusCode();
      doThrow(new RestClientException("not JSON"))
        .when(notFoundExceptionMock)
        .getResponseBodyAs(Error.class);
      doThrow(notFoundExceptionMock)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);

      assertThatThrownBy(() ->
        fixture.findQualityGateConfigByName(TEST_QUALITY_GATE_NAME)
      ).hasMessage(OWN_MESSAGE);
    }

    private void notFoundRejectionCarrying(Error error) {
      var notFoundExceptionMock = mock(RestClientResponseException.class);

      doReturn(NOT_FOUND).when(notFoundExceptionMock).getStatusCode();
      doReturn(error)
        .when(notFoundExceptionMock)
        .getResponseBodyAs(Error.class);
      doThrow(notFoundExceptionMock)
        .when(qualityGateApiMock)
        .getQualityGateByName(TEST_QUALITY_GATE_NAME);
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
