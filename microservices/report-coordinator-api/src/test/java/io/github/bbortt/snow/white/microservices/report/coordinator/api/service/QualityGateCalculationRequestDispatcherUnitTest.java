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

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.Waiver;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

@ExtendWith({ MockitoExtension.class })
class QualityGateCalculationRequestDispatcherUnitTest {

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
    // Order matters here: the constructor reads getCalculationRequestTopic() eagerly to cache it,
    // so the stub must be in place before construction.
    // @InjectMocks would construct the fixture before this @BeforeEach body runs,
    // breaking that ordering - manual construction is kept intentionally.
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

    private static final Instant INITIATED_AT = Instant.parse(
      "2026-04-01T10:15:30Z"
    );

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_004_PER_API_TEST_FAN_OUT_KEYED_BY_CALCULATION_ID
    )
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
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest1, apiTest2)
      );

      verify(kafkaTemplateMock, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_004_PER_API_TEST_FAN_OUT_KEYED_BY_CALCULATION_ID
    )
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

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest)
      );

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

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest)
      );

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

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest)
      );

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

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest)
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      assertThat(
        captor.getValue().value().getAttributeFilters()
      ).containsExactly(new AttributeFilter("env", STRING_EQUALS, "prod"));
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_004_PER_API_TEST_FAN_OUT_KEYED_BY_CALCULATION_ID
    )
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

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest)
      );

      verify(textMapPropagatorMock).inject(any(), any(), any());
    }

    @Test
    void shouldCarryInitiatedAtOnEveryRecord() {
      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(
          apiTest("starWars", "aNewHope", "1"),
          apiTest("starWars", "aNewHope", "2")
        )
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock, times(2)).send(captor.capture());

      assertThat(captor.getAllValues())
        .extracting(producerRecord -> producerRecord.value().getInitiatedAt())
        .containsOnly(INITIATED_AT);
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST
    )
    void shouldLeaveWaiversAbsentWhereTheCalculationWasGivenNone() {
      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest("starWars", "aNewHope", "1"))
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      assertThat(captor.getValue().value().getWaivers()).isNull();
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST
    )
    void shouldCarryOnlyTheWaiversGoverningTheDispatchedApi() {
      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .waivers(
          List.of(
            waiver("starWars", "aNewHope", "1", "/x-wings"),
            waiver("starWars", "theCloneWars", "2", "/clones")
          )
        )
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(
          apiTest("starWars", "aNewHope", "1"),
          apiTest("starWars", "theCloneWars", "2")
        )
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock, times(2)).send(captor.capture());

      assertThat(captor.getAllValues()).allSatisfy(producerRecord ->
        assertThat(producerRecord.value().getWaivers())
          .singleElement()
          .satisfies(dispatchedWaiver ->
            assertThat(dispatchedWaiver.path()).isEqualTo(
              "aNewHope".equals(
                producerRecord.value().getApiInformation().getApiName()
              )
                ? "/x-wings"
                : "/clones"
            )
          )
      );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST
    )
    void shouldCarryAWildcardedWaiverToEveryVersionOfThatApi() {
      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .waivers(List.of(waiver("starWars", "aNewHope", "*", "/x-wings")))
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(
          apiTest("starWars", "aNewHope", "1"),
          apiTest("starWars", "aNewHope", "2"),
          apiTest("starWars", "theCloneWars", "1")
        )
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock, times(3)).send(captor.capture());

      assertThat(captor.getAllValues())
        .filteredOn(producerRecord ->
          "aNewHope".equals(
            producerRecord.value().getApiInformation().getApiName()
          )
        )
        .hasSize(2)
        .allSatisfy(producerRecord ->
          assertThat(producerRecord.value().getWaivers()).hasSize(1)
        );

      assertThat(captor.getAllValues())
        .filteredOn(producerRecord ->
          "theCloneWars".equals(
            producerRecord.value().getApiInformation().getApiName()
          )
        )
        .singleElement()
        .satisfies(producerRecord ->
          assertThat(producerRecord.value().getWaivers()).isNull()
        );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST
    )
    void shouldMapEveryWaiverFieldToTheEvent() {
      var reportParameter = ReportParameter.builder()
        .calculationId(CALCULATION_ID)
        .lookbackWindow("1d")
        .attributeFilters(Map.of())
        .waivers(
          List.of(
            Waiver.builder()
              .serviceName("starWars")
              .apiName("aNewHope")
              .apiVersion("1")
              .path("/x-wings")
              .method("GET")
              .responseCode("418")
              .criterion("RESPONSE_CODE_COVERAGE")
              .reason("The teapot is documented but never flown.")
              .expires(LocalDate.parse("2026-12-31"))
              .build()
          )
        )
        .build();

      mockPropagators();

      fixture.dispatch(
        CALCULATION_ID,
        INITIATED_AT,
        reportParameter,
        Set.of(apiTest("starWars", "aNewHope", "1"))
      );

      ArgumentCaptor<
        ProducerRecord<String, QualityGateCalculationRequestEvent>
      > captor = captor();
      verify(kafkaTemplateMock).send(captor.capture());

      assertThat(captor.getValue().value().getWaivers()).containsExactly(
        new io.github.bbortt.snow.white.commons.event.dto.Waiver(
          "/x-wings",
          "GET",
          "418",
          null,
          null,
          "RESPONSE_CODE_COVERAGE",
          "The teapot is documented but never flown.",
          LocalDate.parse("2026-12-31")
        )
      );
    }

    private static ApiTest apiTest(
      String serviceName,
      String apiName,
      String apiVersion
    ) {
      return ApiTest.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .apiType(OPENAPI.getVal())
        .build();
    }

    private static Waiver waiver(
      String serviceName,
      String apiName,
      String apiVersion,
      String path
    ) {
      return Waiver.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .path(path)
        .method("GET")
        .reason("Known gap, tracked elsewhere.")
        .expires(LocalDate.parse("2026-12-31"))
        .build();
    }
  }
}
