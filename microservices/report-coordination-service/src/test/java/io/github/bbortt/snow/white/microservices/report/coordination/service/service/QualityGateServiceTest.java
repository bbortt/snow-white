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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

  @Test
  void findQualityGateConfigByNameReturnsConfigWhenFound() {
    doReturn(qualityGateDto)
      .when(qualityGateApiMock)
      .getQualityGateByName(TEST_QUALITY_GATE_NAME);
    doReturn(qualityGateConfig)
      .when(qualityGateConfigMapperMock)
      .fromDto(qualityGateDto);

    Optional<QualityGateConfig> result = fixture.findQualityGateConfigByName(
      TEST_QUALITY_GATE_NAME
    );

    assertThat(result).isPresent().get().isEqualTo(qualityGateConfig);
  }

  @Test
  void findQualityGateConfigByNameReturnsEmptyWhenApiReturnsNull() {
    doReturn(null)
      .when(qualityGateApiMock)
      .getQualityGateByName(TEST_QUALITY_GATE_NAME);

    Optional<QualityGateConfig> result = fixture.findQualityGateConfigByName(
      TEST_QUALITY_GATE_NAME
    );

    assertThat(result).isEmpty();
  }

  @Test
  void findQualityGateConfigByNameReturnsEmptyWhenNotFound() {
    var notFoundExceptionMock = mock(RestClientResponseException.class);

    doReturn(NOT_FOUND).when(notFoundExceptionMock).getStatusCode();
    doThrow(notFoundExceptionMock)
      .when(qualityGateApiMock)
      .getQualityGateByName(TEST_QUALITY_GATE_NAME);

    Optional<QualityGateConfig> result = fixture.findQualityGateConfigByName(
      TEST_QUALITY_GATE_NAME
    );

    assertThat(result).isEmpty();
  }

  @Test
  void findQualityGateConfigByNamePropagatesOtherExceptions() {
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
