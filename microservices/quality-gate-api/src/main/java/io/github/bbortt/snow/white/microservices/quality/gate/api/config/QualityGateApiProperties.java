/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties.PREFIX;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = PREFIX)
@Configuration(proxyBeanMethods = false)
public class QualityGateApiProperties {

  public static final String PREFIX = "snow.white.quality.gate.api";

  private String publicApiGatewayUrl;

  /**
   * Location of this instance's quality-gate file, resolved as a Spring resource — on Kubernetes,
   * a path into a mounted {@code ConfigMap}. Its presence is the whole of the switch: set, and
   * every gate on this instance is defined by the file; unset, and gates are owned through the
   * REST surface. There is no second flag to disagree with it, and no gate record carries one.
   */
  @RealizesArch(ArchTraceables.ARCH_025_GITOPS_MODE_IS_INSTANCE_CONFIGURATION)
  private @Nullable String gateFile;
}
