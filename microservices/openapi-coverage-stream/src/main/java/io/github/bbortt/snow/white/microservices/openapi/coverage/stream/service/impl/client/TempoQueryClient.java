/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.annotation.RealizesNf;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.TempoProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition.TempoConfiguredCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
@Conditional(TempoConfiguredCondition.class)
public class TempoQueryClient {

  private static final String SEARCH_PATH = "/api/search";

  private TempoProperties tempoProperties;

  private RestClient tempoRestClient;

  @Autowired
  public void setTempoProperties(TempoProperties tempoProperties) {
    this.tempoProperties = tempoProperties;
  }

  @Autowired
  public void setTempoRestClient(
    @Qualifier("tempoRestClient") RestClient tempoRestClient
  ) {
    this.tempoRestClient = tempoRestClient;
  }

  @Retryable(
    retryFor = {
      HttpServerErrorException.class,
      ResourceAccessException.class,
    },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  @RealizesNf({
    NfTraceables.NF_006_BOUNDED_TELEMETRY_FETCH_FOOTPRINT,
    NfTraceables.NF_007_TEMPO_SEARCH_LIMIT_IS_OPERATOR_CONFIGURABLE,
    NfTraceables.NF_008_TEMPO_SEARCH_RETURNS_EVERY_MATCHED_SPAN_PER_TRACE,
  })
  public JsonNode search(
    String traceQLQuery,
    long startEpochSeconds,
    long endEpochSeconds
  ) {
    // The TraceQL query contains literal '{' / '}' characters, which UriBuilder#queryParam would otherwise misinterpret as URI template placeholders during expansion.
    // Passing it as a template variable instead keeps it an opaque, correctly-encoded value.
    return tempoRestClient
      .get()
      .uri(
        SEARCH_PATH +
          "?q={q}&start={start}&end={end}&limit={limit}&spss={spss}",
        traceQLQuery,
        startEpochSeconds,
        endEpochSeconds,
        tempoProperties.getSearchLimit(),
        tempoProperties.getSpansPerTraceLimit()
      )
      .retrieve()
      .body(JsonNode.class);
  }
}
