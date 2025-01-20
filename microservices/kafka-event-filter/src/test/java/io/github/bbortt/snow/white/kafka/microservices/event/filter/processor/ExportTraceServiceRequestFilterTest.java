package io.github.bbortt.snow.white.kafka.microservices.event.filter.processor;

import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.API_NAME;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.API_NAME_PROPERTY;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.API_VERSION;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.API_VERSION_PROPERTY;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.OTEL_SERVICE_NAME;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.OTEL_SERVICE_NAME_PROPERTY;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITHOUT_API_NAME;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITHOUT_API_VERSION;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITHOUT_OTEL_SERVICE_NAME;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITH_ATTRIBUTES_ON_EACH_LEVEL;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITH_SCOPE_ATTRIBUTES;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.RESOURCE_SPANS_WITH_SPAN_ATTRIBUTES;
import static io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.TestData.wrapResourceSpans;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.kafka.microservices.event.filter.service.CachingService;
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
class ExportTraceServiceRequestFilterTest {

  @Mock
  private CachingService cachingServiceMock;

  private ExportTraceServiceRequestFilter fixture;

  @BeforeEach
  void beforeEachSetup() {
    var kafkaEventFilterProperties = new KafkaEventFilterProperties();
    kafkaEventFilterProperties
      .getFiltering()
      .setApiNameProperty(API_NAME_PROPERTY);
    kafkaEventFilterProperties
      .getFiltering()
      .setApiVersionProperty(API_VERSION_PROPERTY);
    kafkaEventFilterProperties
      .getFiltering()
      .setOtelServiceNameProperty(OTEL_SERVICE_NAME_PROPERTY);

    fixture = new ExportTraceServiceRequestFilter(
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
      var result = fixture.filterUnknownSpecifications(
        exportTraceServiceRequest
      );
      assertFunction.accept(result);
    }

    public static Stream<ResourceSpans> resourceSpansWithValidApiIdentifiers() {
      return Stream.of(
        RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES,
        RESOURCE_SPANS_WITH_SCOPE_ATTRIBUTES,
        RESOURCE_SPANS_WITH_SPAN_ATTRIBUTES,
        RESOURCE_SPANS_WITH_ATTRIBUTES_ON_EACH_LEVEL
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
        RESOURCE_SPANS_WITHOUT_API_NAME,
        RESOURCE_SPANS_WITHOUT_API_VERSION,
        RESOURCE_SPANS_WITHOUT_OTEL_SERVICE_NAME
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
