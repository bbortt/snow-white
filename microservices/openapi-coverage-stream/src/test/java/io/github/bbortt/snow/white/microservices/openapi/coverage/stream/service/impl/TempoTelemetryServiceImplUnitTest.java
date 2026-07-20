/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class TempoTelemetryServiceImplUnitTest {

  private WireMockServer wireMockServer;
  private TempoTelemetryServiceImpl fixture;

  @BeforeEach
  void beforeEachSetup() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    var restClient = RestClient.builder()
      .baseUrl(wireMockServer.baseUrl())
      .build();

    fixture = new TempoTelemetryServiceImpl(
      restClient,
      new OpenApiCoverageStreamProperties()
    );
  }

  @AfterEach
  void afterEachTeardown() {
    wireMockServer.stop();
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
      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(okJson("{\"traces\":[]}"))
      );
    }

    @Test
    void withoutAttributeFilters_shouldBuildCorrectQuery() {
      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet()
      );

      assertThat(result).isEmpty();

      var request = wireMockServer.getAllServeEvents().get(0).getRequest();
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
    void withAttributeFilters_shouldIncludeFiltersInQuery() {
      var filter1 = new AttributeFilter("http.method", STRING_EQUALS, "GET");
      var filter2 = new AttributeFilter(
        "http.status_code",
        STRING_EQUALS,
        "200"
      );
      Set<AttributeFilter> attributeFilters = Set.of(filter1, filter2);

      var result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        attributeFilters
      );

      assertThat(result).isEmpty();

      var request = wireMockServer.getAllServeEvents().get(0).getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).contains(
        "span.http.method = \"GET\"",
        "span.http.status_code = \"200\""
      );
    }

    @Test
    void withoutApiVersion_shouldOmitVersionFilter() {
      var apiInformation = API_INFORMATION.withApiVersion(null);

      fixture.findOpenTelemetryTracingData(
        apiInformation,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet()
      );

      var request = wireMockServer.getAllServeEvents().get(0).getRequest();
      var query = request.getQueryParams().get("q").firstValue();

      assertThat(query).doesNotContain("span.api.version = ");
    }

    @Test
    void withResults_shouldFetchFullSpanAttributesAndParseThem() {
      wireMockServer.resetAll();

      // hex spanID <-> base64 spanId pairs (Tempo returns hex in search
      // results, but base64-encoded bytes in the by-ID OTLP JSON response)
      var spanId1Hex = "3f1a2c9e7d4b8a61";
      var spanId1Base64 = "Pxosnn1LimE=";
      var traceId1 = "f2c79a8d4bce407aa65c1e7289f6febb";

      var spanId2Hex = "8a7d2e4b9c3f1d0a";
      var spanId2Base64 = "in0uS5w/HQo=";
      var traceId2 = "b1e24f988ab04129be3e2cd9275c991a";

      // language=json
      var searchResponseBody = """
        {
          "traces": [
            { "traceID": "%s", "spanSet": { "spans": [{ "spanID": "%s" }] } },
            { "traceID": "%s", "spanSet": { "spans": [{ "spanID": "%s" }] } }
          ]
        }
        """.formatted(traceId1, spanId1Hex, traceId2, spanId2Hex);

      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(
          okJson(searchResponseBody)
        )
      );

      // language=json
      var trace1ResponseBody = """
        {
          "batches": [
            {
              "scopeSpans": [
                {
                  "spans": [
                    {
                      "spanId": "%s",
                      "attributes": [
                        {
                          "key": "http.method",
                          "value": { "stringValue": "GET" }
                        },
                        {
                          "key": "http.path",
                          "value": { "stringValue": "/api/v1/test" }
                        },
                        {
                          "key": "http.status_code",
                          "value": { "intValue": "200" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.formatted(spanId1Base64);

      wireMockServer.stubFor(
        get(urlEqualTo("/api/traces/" + traceId1)).willReturn(
          okJson(trace1ResponseBody)
        )
      );

      // language=json
      var trace2ResponseBody = """
        {
          "batches": [
            {
              "scopeSpans": [
                {
                  "spans": [
                    {
                      "spanId": "%s",
                      "attributes": [
                        {
                          "key": "http.method",
                          "value": { "stringValue": "POST" }
                        },
                        {
                          "key": "http.path",
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
        """.formatted(spanId2Base64);

      wireMockServer.stubFor(
        get(urlEqualTo("/api/traces/" + traceId2)).willReturn(
          okJson(trace2ResponseBody)
        )
      );

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet()
      );

      assertThat(result)
        .hasSize(2)
        .satisfiesExactlyInAnyOrder(
          data1 -> {
            assertThat(data1.spanId()).isEqualTo(spanId1Hex);
            assertThat(data1.traceId()).isEqualTo(traceId1);
            assertThat(
              data1.attributes().get("http.method").asString()
            ).isEqualTo("GET");
            assertThat(
              data1.attributes().get("http.path").asString()
            ).isEqualTo("/api/v1/test");
            assertThat(data1.attributes().has("http.status_code")).isFalse();
          },
          data2 -> {
            assertThat(data2.spanId()).isEqualTo(spanId2Hex);
            assertThat(data2.traceId()).isEqualTo(traceId2);
            assertThat(
              data2.attributes().get("http.method").asString()
            ).isEqualTo("POST");
            assertThat(
              data2.attributes().get("http.path").asString()
            ).isEqualTo("/api/v1/create");
          }
        );
    }

    @Test
    void withLeadingZeroTraceId_shouldZeroPadToFullLength() {
      wireMockServer.resetAll();

      // Tempo's search API strips leading zero nibbles from trace IDs
      // (Jaeger-compatible hex formatting), so a 32-char trace ID starting
      // with a "0" nibble comes back only 31 characters long.
      var spanIdHex = "3f1a2c9e7d4b8a61";
      var spanIdBase64 = "Pxosnn1LimE=";
      var fullTraceId = "069208015c154536b622b34aec8865f6";
      var truncatedTraceId = "69208015c154536b622b34aec8865f6";

      // language=json
      var searchResponseBody = """
        {
          "traces": [
            { "traceID": "%s", "spanSet": { "spans": [{ "spanID": "%s" }] } }
          ]
        }
        """.formatted(truncatedTraceId, spanIdHex);

      wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search")).willReturn(
          okJson(searchResponseBody)
        )
      );

      // language=json
      var traceResponseBody = """
        {
          "batches": [
            {
              "scopeSpans": [
                {
                  "spans": [
                    {
                      "spanId": "%s",
                      "attributes": [
                        {
                          "key": "http.method",
                          "value": { "stringValue": "GET" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.formatted(spanIdBase64);

      wireMockServer.stubFor(
        get(urlEqualTo("/api/traces/" + fullTraceId)).willReturn(
          okJson(traceResponseBody)
        )
      );

      Set<OpenTelemetryData> result = fixture.findOpenTelemetryTracingData(
        API_INFORMATION,
        LOOKBACK_FROM,
        LOOKBACK_WINDOW,
        emptySet()
      );

      assertThat(result)
        .singleElement()
        .satisfies(data -> assertThat(data.traceId()).isEqualTo(fullTraceId));
    }
  }
}
