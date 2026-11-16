/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_DATABASE;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE_EMPTY;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Answers who owns this instance's gates. One question about the instance, asked by the startup
 * step that writes them, by the REST writes that refuse, and by the reads that publish it.
 */
@Service
@RequiredArgsConstructor
public class GateOwnershipService {

  private final QualityGateApiProperties qualityGateApiProperties;
  private final QualityGateConfigurationRepository qualityGateConfigurationRepository;

  /**
   * Whether this instance's gates are file-owned. The configured file's presence is the whole of
   * the switch: there is no second flag to disagree with it, and no gate record carries one, so the
   * answer is the same for every gate the instance holds.
   */
  @RealizesArch(ArchTraceables.ARCH_025_GITOPS_MODE_IS_INSTANCE_CONFIGURATION)
  public boolean isFileOwned() {
    return hasText(qualityGateApiProperties.getGateFile());
  }

  /**
   * The value the gate reads publish. An unmanaged instance is answered without touching the
   * database; only a file-owned one pays for the count, which is what distinguishes an instance
   * whose file defines no gates.
   */
  @RealizesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
  public String currentOwnership() {
    if (!isFileOwned()) {
      return OWNERSHIP_DATABASE;
    }

    return qualityGateConfigurationRepository.count() == 0L
      ? OWNERSHIP_FILE_EMPTY
      : OWNERSHIP_FILE;
  }
}
