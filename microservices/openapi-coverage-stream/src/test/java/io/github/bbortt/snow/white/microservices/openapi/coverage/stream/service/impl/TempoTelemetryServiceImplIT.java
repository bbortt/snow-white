/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractTempoOpenApiCoverageServiceIT;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class TempoTelemetryServiceImplIT
  extends AbstractTempoOpenApiCoverageServiceIT
{

  @Autowired
  private TempoTelemetryServiceImpl tempoTelemetryService;

  @Test
  void shouldFindRealTracingDataIngestedIntoTempo() {
    var serviceName = "tempo-it-service";
    var apiName = "tempo-it-api";
    var apiVersion = "1.0.0";
    var traceId = UUID.randomUUID().toString().replace("-", "");
    var spanId = traceId.substring(0, 16);

    var eventTime = Instant.now();
    var startNanos = eventTime.toEpochMilli() * 1_000_000L;
    var endNanos = startNanos + 1_000_000L;

    ingestTrace(
      serviceName,
      apiName,
      apiVersion,
      traceId,
      spanId,
      startNanos,
      endNanos
    );

    var apiInformation = ApiInformation.builder()
      .serviceName(serviceName)
      .apiName(apiName)
      .apiVersion(apiVersion)
      .apiType(OPENAPI)
      .build();

    await()
      .atMost(ofSeconds(30))
      .untilAsserted(() -> {
        var result = tempoTelemetryService.findOpenTelemetryTracingData(
          apiInformation,
          eventTime.plusSeconds(60).toEpochMilli(),
          "1h",
          Set.of()
        );

        assertThat(result).hasSize(1);

        var data = result.iterator().next();
        assertThat(data.traceId()).isEqualTo(traceId);
        assertThat(data.spanId()).isEqualTo(spanId);
        assertThat(data.attributes().get("http.method").asString()).isEqualTo(
          "GET"
        );
      });
  }

  private static void ingestTrace(
    String serviceName,
    String apiName,
    String apiVersion,
    String traceId,
    String spanId,
    long startNanos,
    long endNanos
  ) {
    var otlpUrl = "http://%s:%s/v1/traces".formatted(
      TEMPO_CONTAINER.getHost(),
      TEMPO_CONTAINER.getMappedPort(TEMPO_OTLP_HTTP_PORT)
    );

    // language=json
    var otlpPayload = """
      {
        "resourceSpans": [
          {
            "resource": {
              "attributes": [
                { "key": "service.name", "value": { "stringValue": "%s" } }
              ]
            },
            "scopeSpans": [
              {
                "scope": { "name": "tempo-it" },
                "spans": [
                  {
                    "traceId": "%s",
                    "spanId": "%s",
                    "name": "GET /api/v1/test",
                    "kind": 2,
                    "startTimeUnixNano": "%d",
                    "endTimeUnixNano": "%d",
                    "attributes": [
                      { "key": "api.name", "value": { "stringValue": "%s" } },
                      {
                        "key": "api.version",
                        "value": { "stringValue": "%s" }
                      },
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
      """.formatted(
      serviceName,
      traceId,
      spanId,
      startNanos,
      endNanos,
      apiName,
      apiVersion
    );

    RestClient.create()
      .post()
      .uri(otlpUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(otlpPayload)
      .retrieve()
      .toBodilessEntity();
  }
}
