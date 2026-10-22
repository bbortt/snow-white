/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.annotation.RealizesNf;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "tempo")
@Configuration(proxyBeanMethods = false)
public class TempoProperties {

  private String url;
  private String username;
  private String password;
  private String token;

  /**
   * Tenant identifier sent as the {@code X-Scope-OrgID} header on every request,
   * for Tempo instances running with multi-tenancy enabled.
   * <p>
   * Optional - omitted entirely when unset,
   * which is correct for single-tenant Tempo instances.
   *
   * @see <a href="https://grafana.com/docs/tempo/latest/operations/manage-advanced-systems/multitenancy/">Tempo multi-tenancy</a>
   */
  private String orgId;

  /**
   * Maximum number of traces a single search may match, sent as the {@code limit} query parameter.
   * <p>
   * Must be a positive integer - validated at startup by
   * {@link io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation.TelemetryBackendPropertiesValidator}.
   *
   * @see <a href="https://grafana.com/docs/tempo/latest/api_docs/">Tempo HTTP API</a>
   */
  @RealizesNf(NfTraceables.NF_007_TEMPO_SEARCH_LIMIT_IS_OPERATOR_CONFIGURABLE)
  private int searchLimit = 1_000;

  /**
   * Maximum number of matched spans each span-set may carry, sent as the {@code spss} query parameter.
   * <p>
   * Sent on every search rather than left to Tempo, whose own default of {@code 3} truncates a
   * trace's matched spans without reporting that it did so.
   * Since the search response is the only
   * source of spans a calculation has, anything it omits is lost outright.
   * <p>
   * The default of {@code 100} matches Tempo's own {@code max_spans_per_span_set} default,
   * making it the largest value a stock Tempo accepts - a request above the server's configured
   * maximum is rejected, not clamped.
   * Lower it to match a deployment that lowered that ceiling;
   * {@code 0} means "no per-span-set limit" and is only accepted by a Tempo whose
   * {@code max_spans_per_span_set} is itself {@code 0}.
   *
   * @see <a href="https://grafana.com/docs/tempo/latest/api_docs/">Tempo HTTP API</a>
   */
  @RealizesNf(
    NfTraceables.NF_008_TEMPO_SEARCH_RETURNS_EVERY_MATCHED_SPAN_PER_TRACE
  )
  private int spansPerTraceLimit = 100;
}
