/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

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
}
