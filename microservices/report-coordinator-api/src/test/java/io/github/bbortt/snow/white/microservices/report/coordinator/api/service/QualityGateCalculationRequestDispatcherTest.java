/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Map;
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
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class QualityGateCalculationRequestDispatcherTest {

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
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  private QualityGateCalculationRequestDispatcher fixture;

  @BeforeEach
  void beforeEach() {
    doReturn(CALCULATION_REQUEST_TOPIC)
      .when(reportCoordinationServicePropertiesMock)
      .getCalculationRequestTopic();

    fixture = new QualityGateCalculationRequestDispatcher(
      kafkaTemplateMock,
      openTelemetryMock,
      reportCoordinationServicePropertiesMock
    );
  }

  private void mockPropagators() {
    doReturn(contextPropagatorsMock).when(openTelemetryMock).getPropagators();
    doReturn(textMapPropagatorMock)
      .when(contextPropagatorsMock)
      .getTextMapPropagator();
  }

  @Nested
  class DispatchTest {

    private static final UUID CALCULATION_ID = UUID.fromString(
      "59d11db1-b07a-4bea-b4c1-8ca178bed839"
    );

    @Test
    void shouldSendOneRecordPerApiTest() {
      var apiTest1 = ApiTest.builder()
        .serviceName("starWars")
        .apiName("aNewHope")
        .apiVersion("1")
        .apiType(OPENAPI.getVal())
        .build();
      var apiTest2 = ApiTest.builder()
        .serviceName("starWars")
        .apiName("theCloneWars")
        .apiVersion("2")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        reportParameter,
        Set.of(apiTest1, apiTest2)
      );

      verify(kafkaTemplateMock, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void shouldRouteToCorrectTopicWithCalculationIdAsKey() {
      var apiTest = ApiTest.builder()
        .serviceName("svc")
        .apiName("api")
        .apiVersion("1")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(CALCULATION_ID, reportParameter, Set.of(apiTest));

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      var recordedQualityGateCalculationRequestEventProducerRecord =
        captor.getValue();
      assertThat(
        recordedQualityGateCalculationRequestEventProducerRecord.topic()
      ).isEqualTo(CALCULATION_REQUEST_TOPIC);
      assertThat(
        recordedQualityGateCalculationRequestEventProducerRecord.key()
      ).isEqualTo(CALCULATION_ID.toString());
    }

    @Test
    void shouldMapApiTestFieldsToApiInformation() {
      var apiTest = ApiTest.builder()
        .serviceName("my-service")
        .apiName("my-api")
        .apiVersion("3.0.0")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("7d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(CALCULATION_ID, reportParameter, Set.of(apiTest));

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      var apiInformation = captor.getValue().value().getApiInformation();
      assertThat(apiInformation.getServiceName()).isEqualTo("my-service");
      assertThat(apiInformation.getApiName()).isEqualTo("my-api");
      assertThat(apiInformation.getApiVersion()).isEqualTo("3.0.0");
      assertThat(apiInformation.getApiType()).isEqualTo(OPENAPI);
    }

    @Test
    void shouldMapLookbackWindowToEvent() {
      var apiTest = ApiTest.builder()
        .serviceName("svc")
        .apiName("api")
        .apiVersion("1")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("30d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(CALCULATION_ID, reportParameter, Set.of(apiTest));

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      assertThat(captor.getValue().value().getLookbackWindow()).isEqualTo(
        "30d"
      );
    }

    @Test
    void shouldMapAttributeFiltersToEvent() {
      var apiTest = ApiTest.builder()
        .serviceName("svc")
        .apiName("api")
        .apiVersion("1")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of("env", "prod"))
        .build();

      mockPropagators();

      fixture.dispatch(CALCULATION_ID, reportParameter, Set.of(apiTest));

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      assertThat(
        captor.getValue().value().getAttributeFilters()
      ).containsExactly(new AttributeFilter("env", STRING_EQUALS, "prod"));
    }

    @Test
    void shouldInjectOtelContextIntoRecordHeaders() {
      var apiTest = ApiTest.builder()
        .serviceName("svc")
        .apiName("api")
        .apiVersion("1")
        .apiType(OPENAPI.getVal())
        .build();

      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(CALCULATION_ID, reportParameter, Set.of(apiTest));

      verify(textMapPropagatorMock).inject(any(), any(), any());
    }
  }
}
