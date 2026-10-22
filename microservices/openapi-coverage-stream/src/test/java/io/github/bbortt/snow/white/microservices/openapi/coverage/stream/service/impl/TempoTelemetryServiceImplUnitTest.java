/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesNf;
import clew.traceables.clew.annotation.RealizesSw;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.TempoProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.TempoQueryClient;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class TempoTelemetryServiceImplUnitTest {

  /**
   * Insertion-ordered, because the {@code select()} clause lists the keys in iteration order.
   */
  private static final Set<String> REQUIRED_ATTRIBUTE_KEYS =
    new LinkedHashSet<>(
      List.of("http.request.method", "url.path", "http.request.header.accept")
    );

  private static final String EXPECTED_SELECT_CLAUSE =
    " | select(span.\"http.request.method\", span.\"url.path\", span.\"http.request.header.accept\")";

  private WireMockServer wireMockServer;
  private TempoProperties tempoProperties;
  private TempoTelemetryServiceImpl fixture;

  @BeforeEach
  void beforeEachSetup() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    var restClient = RestClient.builder()
      .baseUrl(wireMockServer.baseUrl())
      .build();

    tempoProperties = new TempoProperties();

    var tempoQueryClient = new TempoQueryClient();
    tempoQueryClient.setTempoProperties(tempoProperties);
    tempoQueryClient.setTempoRestClient(restClient);

    fixture = new TempoTelemetryServiceImpl(
      tempoQueryClient,
      new OpenApiCoverageStreamProperties()
    );
  }

  @AfterEach
  void afterEachTeardown() {
    wireMockServer.stop();
  }

  private void stubSearchResponse(String body) {
    wireMockServer.stubFor(
      get(urlPathEqualTo("/api/search")).willReturn(okJson(body))
    );
  }

  @Nested
  class FindOpenTelemetryTracingDataTest {

    private static final ApiInformation API_INFORMATION =
      defaultApiInformation();

    // 2024-01-01T00:00:00Z
    private static final long LOOKBACK_FROM = 1_704_067_200_000L;
    private static final String LOOKBACK_WINDOW = "1h";

    @BeforeEach
    void beforeEachSetup() {
      stubSearchResponse("{\"traces\":[]}");
    }

    @Test
    void withoutAttributeFilters_shouldBuildCorrectQuery()
      throws TelemetryBackendUnavailableException {
      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).contains(
        "resource.service.name = \"" + API_INFORMATION.getServiceName() + "\"",
        "span.api.name = \"" + API_INFORMATION.getApiName() + "\"",
        "span.api.version = \"" + API_INFORMATION.getApiVersion() + "\""
      );

      assertThat(request.getQueryParams().get("start").firstValue()).isEqualTo(
        "1704063600"
      );
      assertThat(request.getQueryParams().get("end").firstValue()).isEqualTo(
        "1704067200"
      );
    }

    @Test
    @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
    void withRequiredAttributeKeys_shouldSelectExactlyThoseKeys()
      throws TelemetryBackendUnavailableException {
      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).endsWith(EXPECTED_SELECT_CLAUSE);
    }

    @Test
    void withoutRequiredAttributeKeys_shouldOmitSelectClause()
      throws TelemetryBackendUnavailableException {
      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        emptySet()
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).doesNotContain("select(").endsWith("}");
    }

    @Test
    @RealizesNf({
      NfTraceables.NF_007_TEMPO_SEARCH_LIMIT_IS_OPERATOR_CONFIGURABLE,
      NfTraceables.NF_008_TEMPO_SEARCH_RETURNS_EVERY_MATCHED_SPAN_PER_TRACE,
    })
    void withDefaultProperties_shouldSendBothBounds()
      throws TelemetryBackendUnavailableException {
      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();

      assertThat(request.getQueryParams().get("limit").firstValue()).isEqualTo(
        "1000"
      );
      assertThat(request.getQueryParams().get("spss").firstValue()).isEqualTo(
        "100"
      );
    }

    @Test
    @RealizesNf({
      NfTraceables.NF_007_TEMPO_SEARCH_LIMIT_IS_OPERATOR_CONFIGURABLE,
      NfTraceables.NF_008_TEMPO_SEARCH_RETURNS_EVERY_MATCHED_SPAN_PER_TRACE,
    })
    void withConfiguredProperties_shouldSendConfiguredBounds()
      throws TelemetryBackendUnavailableException {
      tempoProperties.setSearchLimit(17);
      tempoProperties.setSpansPerTraceLimit(42);

      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();

      assertThat(request.getQueryParams().get("limit").firstValue()).isEqualTo(
        "17"
      );
      assertThat(request.getQueryParams().get("spss").firstValue()).isEqualTo(
        "42"
      );
    }

    @Test
    void withAttributeFilters_shouldIncludeFiltersInQuery()
      throws TelemetryBackendUnavailableException {
      var filter1 = new AttributeFilter(
        "service.version",
        STRING_EQUALS,
        "1.2.3"
      );
      var filter2 = new AttributeFilter(
        "environment",
        STRING_EQUALS,
        "development"
      );
      Set<AttributeFilter> attributeFilters = Set.of(filter1, filter2);

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        attributeFilters,
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).contains(
        "resource.service.version = \"1.2.3\"",
        "resource.environment = \"development\""
      );
    }

    @Test
    void withoutApiVersion_shouldOmitVersionFilter()
      throws TelemetryBackendUnavailableException {
      var apiInformation = API_INFORMATION.withApiVersion(null);

      fixture.findOpenTelemetryTracingData(
        apiInformation,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).doesNotContain("span.api.version = ");
    }

    @Test
    @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
    void withResults_shouldParseAttributesFromTheSearchResponseAlone()
      throws TelemetryBackendUnavailableException {
      var spanId1 = "3f1a2c9e7d4b8a61";
      var traceId1 = "f2c79a8d4bce407aa65c1e7289f6febb";

      var spanId2 = "8a7d2e4b9c3f1d0a";
      var traceId2 = "b1e24f988ab04129be3e2cd9275c991a";

      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "%s",
              "spanSets": [
                {
                  "spans": [
                    {
                      "spanID": "%s",
                      "attributes": [
                        {
                          "key": "http.request.method",
                          "value": { "stringValue": "GET" }
                        },
                        {
                          "key": "url.path",
                          "value": { "stringValue": "/api/v1/test" }
                        },
                        {
                          "key": "http.response.status_code",
                          "value": { "intValue": "200" }
                        },
                        {
                          "key": "http.request.duration_ms",
                          "value": { "doubleValue": 12.5 }
                        },
                        {
                          "key": "http.response.cache_hit",
                          "value": { "boolValue": true }
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              "traceID": "%s",
              "spanSets": [
                {
                  "spans": [
                    {
                      "spanID": "%s",
                      "attributes": [
                        {
                          "key": "http.request.method",
                          "value": { "stringValue": "POST" }
                        },
                        {
                          "key": "url.path",
                          "value": { "stringValue": "/api/v1/create" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.formatted(traceId1, spanId1, traceId2, spanId2)
      );

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .hasSize(2)
        .satisfiesExactlyInAnyOrder(
          data1 -> {
            assertThat(data1.spanId()).isEqualTo(spanId1);
            assertThat(data1.traceId()).isEqualTo(traceId1);
            assertThat(
              data1.attributes().get("http.request.method").asString()
            ).isEqualTo("GET");
            assertThat(data1.attributes().get("url.path").asString()).isEqualTo(
              "/api/v1/test"
            );
            assertThat(
              data1.attributes().get("http.response.status_code").asString()
            ).isEqualTo("200");
            assertThat(
              data1.attributes().get("http.request.duration_ms").asString()
            ).isEqualTo("12.5");
            assertThat(
              data1.attributes().get("http.response.cache_hit").asString()
            ).isEqualTo("true");
          },
          data2 -> {
            assertThat(data2.spanId()).isEqualTo(spanId2);
            assertThat(data2.traceId()).isEqualTo(traceId2);
            assertThat(
              data2.attributes().get("http.request.method").asString()
            ).isEqualTo("POST");
            assertThat(data2.attributes().get("url.path").asString()).isEqualTo(
              "/api/v1/create"
            );
          }
        );
    }

    @Test
    @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
    void shouldNeverFetchATraceById()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "f2c79a8d4bce407aa65c1e7289f6febb",
              "spanSets": [{ "spans": [{ "spanID": "3f1a2c9e7d4b8a61" }] }]
            }
          ]
        }
        """
      );

      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      wireMockServer.verify(
        0,
        com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
          urlPathMatching("/api/v2/traces/.*")
        )
      );
    }

    @Test
    @RealizesSw(SwTraceables.SW_022_TEMPO_SEARCH_RETURNS_ONLY_REQUIRED_KEYS)
    void withBothSpanSetsAndDeprecatedSpanSet_shouldYieldEachSpanOnce()
      throws TelemetryBackendUnavailableException {
      var spanId = "3f1a2c9e7d4b8a61";
      var traceId = "f2c79a8d4bce407aa65c1e7289f6febb";

      // Tempo populates both fields with the same spans; the singular one is deprecated.
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "%s",
              "spanSets": [{ "spans": [{ "spanID": "%s" }] }],
              "spanSet": { "spans": [{ "spanID": "%s" }] }
            }
          ]
        }
        """.formatted(traceId, spanId, spanId)
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> assertThat(data.spanId()).isEqualTo(spanId));
    }

    @Test
    void withOnlyTheDeprecatedSpanSet_shouldStillYieldItsSpans()
      throws TelemetryBackendUnavailableException {
      var spanId = "3f1a2c9e7d4b8a61";
      var traceId = "f2c79a8d4bce407aa65c1e7289f6febb";

      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            { "traceID": "%s", "spanSet": { "spans": [{ "spanID": "%s" }] } }
          ]
        }
        """.formatted(traceId, spanId)
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> assertThat(data.spanId()).isEqualTo(spanId));
    }

    @Test
    @RealizesNf(
      NfTraceables.NF_008_TEMPO_SEARCH_RETURNS_EVERY_MATCHED_SPAN_PER_TRACE
    )
    void withMoreSpansThanTemposOwnDefault_shouldYieldAllOfThem()
      throws TelemetryBackendUnavailableException {
      var spanIds = List.of(
        "3f1a2c9e7d4b8a61",
        "8a7d2e4b9c3f1d0a",
        "1a2b3c4d5e6f7081",
        "9182736455647382",
        "5c6d7e8f90a1b2c3"
      );

      var spans = spanIds
        .stream()
        .map(spanId -> "{ \"spanID\": \"" + spanId + "\" }")
        .collect(java.util.stream.Collectors.joining(", "));

      stubSearchResponse(
        """
        {
          "traces": [
            {
              "traceID": "f2c79a8d4bce407aa65c1e7289f6febb",
              "spanSets": [{ "spans": [%s] }]
            }
          ]
        }
        """.formatted(spans)
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .hasSize(spanIds.size())
        .map(OpenTelemetryData::spanId)
        .containsExactlyInAnyOrderElementsOf(spanIds);
    }

    @Test
    void withMultipleSpanSets_shouldYieldEverySpanSetsSpans()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "f2c79a8d4bce407aa65c1e7289f6febb",
              "spanSets": [
                { "spans": [{ "spanID": "3f1a2c9e7d4b8a61" }] },
                { "spans": [{ "spanID": "8a7d2e4b9c3f1d0a" }] }
              ]
            }
          ]
        }
        """
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .hasSize(2)
        .map(OpenTelemetryData::spanId)
        .containsExactlyInAnyOrder("3f1a2c9e7d4b8a61", "8a7d2e4b9c3f1d0a");
    }

    @Test
    void withLeadingZeroTraceId_shouldZeroPadToFullLength()
      throws TelemetryBackendUnavailableException {
      // Tempo's search API strips leading zero nibbles from trace IDs
      // (Jaeger-compatible hex formatting), so a 32-char trace ID starting
      // with a "0" nibble comes back only 31 characters long.
      var fullTraceId = "069208015c154536b622b34aec8865f6";
      var truncatedTraceId = "69208015c154536b622b34aec8865f6";

      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            { "traceID": "%s", "spanSets": [{ "spans": [{ "spanID": "%s" }] }] }
          ]
        }
        """.formatted(truncatedTraceId, "3f1a2c9e7d4b8a61")
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> assertThat(data.traceId()).isEqualTo(fullTraceId));
    }

    @Test
    void withUnparseableLookbackWindow_shouldThrowIllegalArgumentException() {
      assertThatThrownBy(() ->
        fixture.findOpenTelemetryTracingData(
          API_INFORMATION,
          LOOKBACK_FROM,
          "not-a-window",
          emptySet(),
          REQUIRED_ATTRIBUTE_KEYS
        )
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unparseable lookback window");
    }

    static Stream<Arguments> lookbackWindowsAndExpectedStartEpochSeconds() {
      return Stream.of(
        arguments("1ms", "1704067199"),
        arguments("1s", "1704067199"),
        arguments("1m", "1704067140"),
        arguments("1d", "1703980800"),
        arguments("1w", "1703462400")
      );
    }

    @ParameterizedTest
    @MethodSource("lookbackWindowsAndExpectedStartEpochSeconds")
    void withVariousLookbackWindowUnits_shouldComputeCorrectStartEpochSeconds(
      String lookbackWindow,
      String expectedStartEpochSeconds
    ) throws TelemetryBackendUnavailableException {
      fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        lookbackWindow,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      var request = wireMockServer.getAllServeEvents().getFirst().getRequest();
      assertThat(request.getQueryParams().get("start").firstValue()).isEqualTo(
        expectedStartEpochSeconds
      );
    }

    @Test
    void withSearchResponseMissingTracesProperty_shouldReturnEmptyResult()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse("{}");

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();
    }

    @Test
    void withEmptySearchResponseBody_shouldReturnEmptyResult()
      throws TelemetryBackendUnavailableException {
      wireMockServer.resetAll();
      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(
          aResponse().withStatus(200)
        )
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();
    }

    @Test
    void withTraceMissingSpanSet_shouldSkipTrace()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        { "traces": [{ "traceID": "f2c79a8d4bce407aa65c1e7289f6febb" }] }
        """
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();
    }

    @Test
    void withSpanSetMissingSpansProperty_shouldSkipSpanSet()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            { "traceID": "f2c79a8d4bce407aa65c1e7289f6febb", "spanSets": [{}] }
          ]
        }
        """
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result).isEmpty();
    }

    @Test
    void withSpanMissingAttributesProperty_shouldResultInEmptyAttributes()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "f2c79a8d4bce407aa65c1e7289f6febb",
              "spanSets": [{ "spans": [{ "spanID": "3f1a2c9e7d4b8a61" }] }]
            }
          ]
        }
        """
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> assertThat(data.attributes().isEmpty()).isTrue());
    }

    @Test
    void withAttributeMissingValueProperty_shouldSkipAttribute()
      throws TelemetryBackendUnavailableException {
      stubSearchResponse(
        // language=json
        """
        {
          "traces": [
            {
              "traceID": "f2c79a8d4bce407aa65c1e7289f6febb",
              "spanSets": [
                {
                  "spans": [
                    {
                      "spanID": "3f1a2c9e7d4b8a61",
                      "attributes": [
                        { "key": "http.request.method" },
                        {
                          "key": "url.path",
                          "value": { "stringValue": "/api/v1/test" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
        """
      );

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet(),
        REQUIRED_ATTRIBUTE_KEYS
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> {
          assertThat(data.attributes().has("http.request.method")).isFalse();
          assertThat(data.attributes().get("url.path").asString()).isEqualTo(
            "/api/v1/test"
          );
        });
    }
  }

  @Nested
  class FindOpenTelemetryTracingDataExceptionHandlingTest {

    private static final ApiInformation API_INFORMATION =
      defaultApiInformation();

    // 2024-01-01T00:00:00Z
    private static final long LOOKBACK_FROM = 1_704_067_200_000L;
    private static final String LOOKBACK_WINDOW = "1h";

    @Test
    void shouldThrowTelemetryBackendUnavailableException_whenTempoRespondsWithServerError() {
      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(
          aResponse().withStatus(502)
        )
      );

      assertThatThrownBy(() ->
        fixture.findOpenTelemetryTracingData(
          API_INFORMATION,
          LOOKBACK_FROM,
          LOOKBACK_WINDOW,
          emptySet(),
          REQUIRED_ATTRIBUTE_KEYS
        )
      )
        .isInstanceOf(TelemetryBackendUnavailableException.class)
        .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void shouldThrowTelemetryBackendUnavailableException_whenTempoIsUnreachable() {
      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(
          aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
        )
      );

      assertThatThrownBy(() ->
        fixture.findOpenTelemetryTracingData(
          API_INFORMATION,
          LOOKBACK_FROM,
          LOOKBACK_WINDOW,
          emptySet(),
          REQUIRED_ATTRIBUTE_KEYS
        )
      )
        .isInstanceOf(TelemetryBackendUnavailableException.class)
        .hasCauseInstanceOf(ResourceAccessException.class);
    }
  }
}
