/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_DATABASE;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE_EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class GateOwnershipServiceUnitTest {

  @Mock
  private QualityGateConfigurationRepository qualityGateConfigurationRepositoryMock;

  private QualityGateApiProperties qualityGateApiProperties;

  private GateOwnershipService fixture;

  @BeforeEach
  void beforeEachSetup() {
    qualityGateApiProperties = new QualityGateApiProperties();

    fixture = new GateOwnershipService(
      qualityGateApiProperties,
      qualityGateConfigurationRepositoryMock
    );
  }

  @Nested
  class IsFileOwnedTest {

    @Test
    @VerifiesArch(ArchTraceables.ARCH_025_GITOPS_MODE_IS_INSTANCE_CONFIGURATION)
    void shouldBeTrue_whenGateFileIsConfigured() {
      qualityGateApiProperties.setGateFile("file:/etc/gates/gates.yaml");

      assertThat(fixture.isFileOwned()).isTrue();
    }

    @ParameterizedTest(name = "{0} → false")
    @NullSource
    @ValueSource(strings = { "", "   " })
    @VerifiesArch(ArchTraceables.ARCH_025_GITOPS_MODE_IS_INSTANCE_CONFIGURATION)
    void shouldBeFalse_whenGateFileIsNotConfigured(@Nullable String gateFile) {
      qualityGateApiProperties.setGateFile(gateFile);

      assertThat(fixture.isFileOwned()).isFalse();
    }
  }

  @Nested
  class CurrentOwnershipTest {

    @Test
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldBeDatabase_withoutTouchingTheRepository_whenInstanceIsUnmanaged() {
      qualityGateApiProperties.setGateFile(null);

      assertThat(fixture.currentOwnership()).isEqualTo(OWNERSHIP_DATABASE);

      verifyNoInteractions(qualityGateConfigurationRepositoryMock);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldBeFileEmpty_whenFileOwnedAndNoGatesExist() {
      qualityGateApiProperties.setGateFile("file:/etc/gates/gates.yaml");
      doReturn(0L).when(qualityGateConfigurationRepositoryMock).count();

      assertThat(fixture.currentOwnership()).isEqualTo(OWNERSHIP_FILE_EMPTY);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldBeFile_whenFileOwnedAndOneGateExists() {
      qualityGateApiProperties.setGateFile("file:/etc/gates/gates.yaml");
      doReturn(1L).when(qualityGateConfigurationRepositoryMock).count();

      assertThat(fixture.currentOwnership()).isEqualTo(OWNERSHIP_FILE);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldBeFile_whenFileOwnedAndManyGatesExist() {
      qualityGateApiProperties.setGateFile("file:/etc/gates/gates.yaml");
      doReturn(4L).when(qualityGateConfigurationRepositoryMock).count();

      assertThat(fixture.currentOwnership()).isEqualTo(OWNERSHIP_FILE);
    }
  }
}
