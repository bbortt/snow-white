/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.API_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.API_VERSION;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.OTEL_SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.wrapResourceSpans;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OtelInformationFilteringServiceTest {

  private static final String SERVICE_NAME_PROPERTY =
    "OtelInformationFilteringServiceTest:serviceName";
  private static final String API_NAME_PROPERTY =
    "OtelInformationFilteringServiceTest:apiName";
  private static final String API_VERSION_PROPERTY =
    "OtelInformationFilteringServiceTest:apiVersion";

  private static final TestData TEST_DATA = TestData.builder()
    .serviceNameProperty(SERVICE_NAME_PROPERTY)
    .apiNameProperty(API_NAME_PROPERTY)
    .apiVersionProperty(API_VERSION_PROPERTY)
    .build();

  @Mock
  private CachingService cachingServiceMock;

  private OtelInformationFilteringService fixture;

  @BeforeEach
  void beforeEachSetup() {
    var kafkaEventFilterProperties = new OtelEventFilterStreamProperties();
    kafkaEventFilterProperties
      .getFiltering()
      .setApiNameProperty(API_NAME_PROPERTY);
    kafkaEventFilterProperties
      .getFiltering()
      .setApiVersionProperty(API_VERSION_PROPERTY);
    kafkaEventFilterProperties
      .getFiltering()
      .setServiceNameProperty(SERVICE_NAME_PROPERTY);

    fixture = new OtelInformationFilteringService(
      cachingServiceMock,
      kafkaEventFilterProperties
    );
  }

  @Nested
  class Constructor {

    @Test
    void extractsProperties() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class FilterUnknownSpecifications {

    private void filterUnknownSpecificationsAndAssert(
      ExportTraceServiceRequest exportTraceServiceRequest,
      Consumer<ExportTraceServiceRequest> assertFunction
    ) {
      ExportTraceServiceRequest result = fixture.filterUnknownSpecifications(
        exportTraceServiceRequest
      );
      assertFunction.accept(result);
    }

    public static Stream<ResourceSpans> resourceSpansWithValidApiIdentifiers() {
      return Stream.of(
        TEST_DATA.resourceSpansWithResourceAttributes(),
        TEST_DATA.resourceSpansWithScopeAttributes(),
        TEST_DATA.resourceSpansWithSpanAttributes(),
        TEST_DATA.resourceSpansWithAttributesOnEachLevel()
      );
    }

    @ParameterizedTest
    @MethodSource("resourceSpansWithValidApiIdentifiers")
    void returnsValidExportRaceServiceRequestOfKnownApi(
      ResourceSpans resourceSpans
    ) {
      doReturn(true)
        .when(cachingServiceMock)
        .apiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

      filterUnknownSpecificationsAndAssert(
        wrapResourceSpans(resourceSpans),
        exportTraceServiceRequest ->
          assertThat(exportTraceServiceRequest)
            .isNotNull()
            .extracting(ExportTraceServiceRequest::getResourceSpansList)
            .asInstanceOf(LIST)
            .isNotEmpty()
            .first()
            .asInstanceOf(type(ResourceSpans.class))
            .isEqualTo(resourceSpans)
      );
    }

    @ParameterizedTest
    @MethodSource("resourceSpansWithValidApiIdentifiers")
    void doesNotReturnValidExportTraceServiceRequestOfUnknownApi(
      ResourceSpans resourceSpans
    ) {
      doReturn(false)
        .when(cachingServiceMock)
        .apiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

      filterUnknownSpecificationsAndAssert(
        wrapResourceSpans(resourceSpans),
        exportTraceServiceRequest ->
          assertThat(exportTraceServiceRequest)
            .isNotNull()
            .extracting(ExportTraceServiceRequest::getResourceSpansList)
            .asInstanceOf(LIST)
            .isNotEmpty()
            .first()
            .asInstanceOf(type(ResourceSpans.class))
            .isNotNull()
      );
    }

    public static Stream<ResourceSpans> invalidResourceSpans() {
      return Stream.of(
        TEST_DATA.resourceSpansWithoutApiName(),
        TEST_DATA.resourceSpansWithoutApiVersion(),
        TEST_DATA.resourceSpansWithoutOtelServiceName()
      );
    }

    @ParameterizedTest
    @MethodSource("invalidResourceSpans")
    void doesNotReturnInvalidExportTraceServiceRequest(
      ResourceSpans resourceSpans
    ) {
      filterUnknownSpecificationsAndAssert(
        wrapResourceSpans(resourceSpans),
        exportTraceServiceRequest ->
          assertThat(exportTraceServiceRequest)
            .isNotNull()
            .extracting(ExportTraceServiceRequest::getResourceSpansList)
            .asInstanceOf(LIST)
            .isNotEmpty()
            .first()
            .asInstanceOf(type(ResourceSpans.class))
            .isNotNull()
            .extracting(ResourceSpans::getScopeSpansList)
            .asInstanceOf(LIST)
            .isNotEmpty()
            .first()
            .asInstanceOf(type(ScopeSpans.class))
            .isNotNull()
            .extracting(ScopeSpans::getSpansList)
            .asInstanceOf(LIST)
            .isEmpty()
      );

      verifyNoInteractions(cachingServiceMock);
    }
  }
}
