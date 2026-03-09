/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith({ MockitoExtension.class })
class ReportServiceTest {

  private static final String CALCULATION_REQUEST_TOPIC =
    "calculation-request-topic";

  @Mock
  private KafkaTemplate<
    @NonNull String,
    @NonNull QualityGateCalculationRequestEvent
  > kafkaTemplateMock;

  @Mock
  private OpenTelemetry openTelemetryMock;

  @Mock
  private ContextPropagators contextPropagatorsMock;

  @Mock
  private TextMapPropagator textMapPropagatorMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  @Mock
  private ApiTestRepository apiTestRepositoryMock;

  @Mock
  private QualityGateReportRepository qualityGateReportRepositoryMock;

  @Mock
  private ApiTestResultMapper apiTestResultMapperMock;

  @Mock
  private ApiInformationFilter apiInformationFilterMock;

  @Mock
  private ApiTestResultLinker apiTestResultLinkerMock;

  @Mock
  private QualityGateStatusCalculator qualityGateStatusCalculatorMock;

  private ReportService fixture;

  @BeforeEach
  void beforeEachSetup() {
    var reportCoordinationServiceProperties =
      new ReportCoordinationServiceProperties();
    reportCoordinationServiceProperties.setCalculationRequestTopic(
      CALCULATION_REQUEST_TOPIC
    );

    fixture = new ReportService(
      kafkaTemplateMock,
      openTelemetryMock,
      qualityGateServiceMock,
      apiTestRepositoryMock,
      qualityGateReportRepositoryMock,
      reportCoordinationServiceProperties,
      apiTestResultMapperMock,
      apiInformationFilterMock,
      apiTestResultLinkerMock,
      qualityGateStatusCalculatorMock
    );
  }

  @Nested
  class FindReportByCalculationId {

    @Test
    void shouldReturnQualityGateReportById() {
      var reportCalculationId = UUID.fromString(
        "85b5b089-9fe0-46c2-9fa4-e7eaaf33e0d3"
      );

      var qualityGateReport = new QualityGateReport();
      doReturn(Optional.of(qualityGateReport))
        .when(qualityGateReportRepositoryMock)
        .findById(reportCalculationId);

      var result = fixture.findReportByCalculationId(reportCalculationId);

      assertThat(result).isPresent().get().isEqualTo(qualityGateReport);
    }
  }

  @Nested
  class UpdateReportWithOpenApiCoverageResults {

    private static final UUID CALCULATION_ID = UUID.fromString(
      "9f679723-a328-47c6-b24e-e16894c675f1"
    );
    private static final String QUALITY_GATE_CONFIG_NAME = "test-config";

    @Test
    void shouldUpdateReport_whenReportAndQualityGateConfigExist()
      throws QualityGateNotFoundException {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .reportParameter(mock(ReportParameter.class))
        .build();

      var updatedReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(PASSED)
        .reportParameter(mock(ReportParameter.class))
        .build();

      doReturn(Optional.of(originalReport))
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      var qualityGateConfig = new QualityGateConfig(
        QUALITY_GATE_CONFIG_NAME,
        Set.of("PATH_COVERAGE")
      );
      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);

      doReturn(apiTest)
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          originalReport,
          apiInformation
        );

      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        emptySet()
      );

      Set<ApiTestResult> mappedResults = Set.of(mock(ApiTestResult.class));

      doReturn(mappedResults)
        .when(apiTestResultMapperMock)
        .fromDtos(emptySet(), apiTest);
      doReturn(updatedReport)
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(originalReport);

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verify(
        apiInformationFilterMock
      ).findApiTestMatchingApiInformationInQualityGateReport(
        originalReport,
        apiInformation
      );
      verify(qualityGateServiceMock).findQualityGateConfigByName(
        QUALITY_GATE_CONFIG_NAME
      );
      verify(apiTestResultMapperMock).fromDtos(emptySet(), apiTest);
      verify(apiTestResultLinkerMock).addApiTestResultsToApiTest(
        mappedResults,
        apiTest,
        qualityGateConfig.getOpenApiCriteria()
      );
      verify(qualityGateStatusCalculatorMock).withUpdatedReportStatus(
        originalReport
      );
      verify(qualityGateReportRepositoryMock).save(updatedReport);
    }

    @Test
    void shouldLogWarningAndReturn_whenReportDoesNotExist()
      throws QualityGateNotFoundException {
      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        mock(ApiInformation.class),
        emptySet()
      );

      doReturn(Optional.empty())
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verifyNoInteractions(apiInformationFilterMock);
      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(qualityGateReportRepositoryMock, never()).save(any());
    }
  }

  @Nested
  class InitializeQualityGateCalculation {

    private void mockPropagators() {
      doReturn(contextPropagatorsMock).when(openTelemetryMock).getPropagators();
      doReturn(textMapPropagatorMock)
        .when(contextPropagatorsMock)
        .getTextMapPropagator();
    }

    @Test
    void shouldCreateAndReturnQualityGateReport_andPersistCorrectValues()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "test-config";
      var apiTest = ApiTest.builder()
        .serviceName("test-service")
        .apiName("test-api")
        .apiVersion("test-api-version")
        .build();
      var apiTests = Set.of(apiTest);

      var reportParameter = ReportParameter.builder()
        .calculationId(UUID.fromString("41386eb3-7569-4944-a39f-0bdcadf15654"))
        .lookbackWindow("1d")
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        emptySet()
      );

      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      var savedQualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("6f465636-2ea3-4279-80db-6ff1643df6af"))
        .reportParameter(reportParameter)
        .build();
      doReturn(savedQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(any(QualityGateReport.class));

      doAnswer(returnsFirstArg())
        .when(apiTestRepositoryMock)
        .save(any(ApiTest.class));

      mockPropagators();

      QualityGateReport initializedQualityGateReport =
        fixture.initializeQualityGateCalculation(
          qualityGateConfigName,
          apiTests,
          reportParameter
        );

      assertThat(initializedQualityGateReport.getApiTests())
        .hasSize(1)
        .allSatisfy(preparedApiTest ->
          assertThat(preparedApiTest.getQualityGateReport()).isEqualTo(
            savedQualityGateReport
          )
        );

      ArgumentCaptor<QualityGateReport> reportCaptor = captor();
      verify(qualityGateReportRepositoryMock).save(reportCaptor.capture());

      var initialQualityGate = reportCaptor.getValue();
      assertThat(initialQualityGate).satisfies(
        qualityGateReport ->
          assertThat(qualityGateReport.getQualityGateConfigName()).isEqualTo(
            qualityGateConfigName
          ),
        qualityGateReport ->
          assertThat(qualityGateReport.getReportParameter()).satisfies(
            initialReportParameter ->
              assertThat(initialReportParameter.getCalculationId()).isEqualTo(
                initialQualityGate.getCalculationId()
              ),
            initialReportParameter ->
              assertThat(initialReportParameter.getLookbackWindow()).isEqualTo(
                reportParameter.getLookbackWindow()
              )
          ),
        qualityGateReport ->
          assertThat(qualityGateReport.getApiTests()).isEmpty(),
        qualityGateReport ->
          assertThat(qualityGateReport.getReportStatus()).isEqualTo(IN_PROGRESS)
      );

      ArgumentCaptor<ApiTest> apiTestCaptor = captor();
      verify(apiTestRepositoryMock).save(apiTestCaptor.capture());

      assertThat(apiTestCaptor.getAllValues())
        .hasSize(1)
        .allSatisfy(persistedApiTest ->
          assertThat(persistedApiTest.getQualityGateReport()).isEqualTo(
            savedQualityGateReport
          )
        );
    }

    @Test
    void shouldCreateAndReturnQualityGateReport_andDispatchKafkaEvent()
      throws QualityGateNotFoundException {
      assertThatKafkaEventIsBeingDispatched(Map.of());
    }

    @Test
    void shouldCreateAndReturnQualityGateReport_andDispatchKafkaEvent_withFilterAttributes()
      throws QualityGateNotFoundException {
      assertThat(
        assertThatKafkaEventIsBeingDispatched(Map.of("key", "value"))
      ).allSatisfy(event ->
        assertThat(event.getAttributeFilters()).containsExactly(
          new AttributeFilter("key", STRING_EQUALS, "value")
        )
      );
    }

    List<
      QualityGateCalculationRequestEvent
    > assertThatKafkaEventIsBeingDispatched(
      Map<String, String> attributeFilters
    ) throws QualityGateNotFoundException {
      var apiTest1 = ApiTest.builder()
        .serviceName("starWars")
        .apiName("aNewHope")
        .apiVersion("1")
        .build();
      var apiTest2 = ApiTest.builder()
        .serviceName("starWars")
        .apiName("theCloneWars")
        .apiVersion("2")
        .build();
      var apiTest = Set.of(apiTest1, apiTest2);

      var qualityGateConfigName = "test-config";
      var reportParameter = ReportParameter.builder()
        .calculationId(UUID.fromString("59d11db1-b07a-4bea-b4c1-8ca178bed839"))
        .lookbackWindow("1d")
        .attributeFilters(attributeFilters)
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        emptySet()
      );

      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      var initialQualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("a1c937c2-d950-4047-8c8d-f1de16c13b41"))
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameter(reportParameter)
        .build();

      doReturn(initialQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(any(QualityGateReport.class));

      doAnswer(returnsFirstArg())
        .when(apiTestRepositoryMock)
        .save(any(ApiTest.class));

      mockPropagators();

      var result = fixture.initializeQualityGateCalculation(
        qualityGateConfigName,
        apiTest,
        reportParameter
      );

      assertThat(result).isNotNull();

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > producerRecordCaptor = captor();

      verify(kafkaTemplateMock, times(2)).send(producerRecordCaptor.capture());

      var capturedEvents = producerRecordCaptor
        .getAllValues()
        .stream()
        .map(ProducerRecord::value)
        .toList();

      assertThat(producerRecordCaptor.getAllValues()).allSatisfy(
        qualityGateCalculationRequestEventProducerRecord -> {
          assertThat(
            qualityGateCalculationRequestEventProducerRecord.topic()
          ).isEqualTo(CALCULATION_REQUEST_TOPIC);
          assertThat(
            qualityGateCalculationRequestEventProducerRecord.key()
          ).isEqualTo(initialQualityGateReport.getCalculationId().toString());
        }
      );

      assertThat(capturedEvents)
        .hasSize(2)
        .satisfiesOnlyOnce(event ->
          assertThat(event)
            .extracting(QualityGateCalculationRequestEvent::getApiInformation)
            .satisfies(
              apiInformation ->
                assertThat(apiInformation.getServiceName()).isEqualTo(
                  apiTest1.getServiceName()
                ),
              apiInformation ->
                assertThat(apiInformation.getApiName()).isEqualTo(
                  apiTest1.getApiName()
                ),
              apiInformation ->
                assertThat(apiInformation.getApiVersion()).isEqualTo(
                  apiTest1.getApiVersion()
                )
            )
        )
        .satisfiesOnlyOnce(event ->
          assertThat(event)
            .extracting(QualityGateCalculationRequestEvent::getApiInformation)
            .satisfies(
              apiInformation ->
                assertThat(apiInformation.getServiceName()).isEqualTo(
                  apiTest2.getServiceName()
                ),
              apiInformation ->
                assertThat(apiInformation.getApiName()).isEqualTo(
                  apiTest2.getApiName()
                ),
              apiInformation ->
                assertThat(apiInformation.getApiVersion()).isEqualTo(
                  apiTest2.getApiVersion()
                )
            )
        )
        .allSatisfy(event ->
          assertThat(event.getLookbackWindow()).isEqualTo(
            reportParameter.getLookbackWindow()
          )
        );

      return capturedEvents;
    }

    @Test
    void shouldThrowQualityGateNotFoundException_whenQualityGateConfigNotFound()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "non-existent-config";
      var reportParameter = new ReportParameter();

      var cause = new QualityGateNotFoundException(qualityGateConfigName);
      doThrow(cause)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      assertThatThrownBy(() ->
        fixture.initializeQualityGateCalculation(
          qualityGateConfigName,
          emptySet(),
          reportParameter
        )
      ).isEqualTo(cause);
    }
  }

  @Test
  void shouldReturnUpdatedQualityGateReport() {
    var qualityGateReport = new QualityGateReport();

    var updatedQualityGateReport = new QualityGateReport();
    doReturn(updatedQualityGateReport)
      .when(qualityGateReportRepositoryMock)
      .save(qualityGateReport);

    var result = fixture.update(qualityGateReport);

    assertThat(result).isEqualTo(updatedQualityGateReport);
  }

  @Nested
  class Update {

    @Test
    void shouldReturnUpdatedQualityGateReport() {
      var qualityGateReport = new QualityGateReport();

      var updatedQualityGateReport = new QualityGateReport();
      doReturn(updatedQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(qualityGateReport);

      var result = fixture.update(qualityGateReport);

      assertThat(result).isEqualTo(updatedQualityGateReport);
    }
  }

  @Nested
  class FindAllReports {

    @Test
    void shouldQueryRepository() {
      var pageable = Pageable.unpaged();

      var qualityGateReports = Page.empty();
      doReturn(qualityGateReports)
        .when(qualityGateReportRepositoryMock)
        .findAll(pageable);

      Page<@NonNull QualityGateReport> result = fixture.findAllReports(
        pageable
      );

      assertThat(result).isEqualTo(qualityGateReports);
    }
  }
}
