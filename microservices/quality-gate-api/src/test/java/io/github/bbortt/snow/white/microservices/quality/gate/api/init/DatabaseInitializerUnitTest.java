/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.init;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.GateOwnershipService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateFileReader;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.QualityGateFileException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class DatabaseInitializerUnitTest {

  @Mock
  private GateOwnershipService gateOwnershipServiceMock;

  @Mock
  private OpenApiCoverageConfigurationService openApiCoverageConfigurationServiceMock;

  @Mock
  private QualityGateFileReader qualityGateFileReaderMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  @InjectMocks
  private DatabaseInitializer fixture;

  @Nested
  class RunTest {

    @Test
    @VerifiesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
    void shouldInitiateDatabaseInitialization() {
      doReturn(false).when(gateOwnershipServiceMock).isFileOwned();

      fixture.run();

      var inOrder = inOrder(
        openApiCoverageConfigurationServiceMock,
        qualityGateServiceMock
      );
      inOrder
        .verify(openApiCoverageConfigurationServiceMock)
        .initOpenApiCoverageCriteria();
      inOrder.verify(qualityGateServiceMock).initPredefinedQualityGates();

      verifyNoInteractions(qualityGateFileReaderMock);
      verify(qualityGateServiceMock, never()).applyQualityGateFile(anyList());
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
    )
    @VerifiesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
    void shouldApplyGateFileAfterCriteria_andNotSeedPredefinedGates_whenFileOwned() {
      doReturn(true).when(gateOwnershipServiceMock).isFileOwned();

      var gates = List.of(
        QualityGateConfig.builder().name("from-file").build()
      );
      doReturn(gates).when(qualityGateFileReaderMock).readQualityGateFile();

      fixture.run();

      var inOrder = inOrder(
        openApiCoverageConfigurationServiceMock,
        qualityGateFileReaderMock,
        qualityGateServiceMock
      );
      inOrder
        .verify(openApiCoverageConfigurationServiceMock)
        .initOpenApiCoverageCriteria();
      inOrder.verify(qualityGateFileReaderMock).readQualityGateFile();
      inOrder.verify(qualityGateServiceMock).applyQualityGateFile(gates);

      verify(qualityGateServiceMock, never()).initPredefinedQualityGates();
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
    )
    void shouldApplyAnEmptyGateFile_whenFileDefinesNoGates() {
      doReturn(true).when(gateOwnershipServiceMock).isFileOwned();
      doReturn(List.of()).when(qualityGateFileReaderMock).readQualityGateFile();

      fixture.run();

      verify(qualityGateServiceMock).applyQualityGateFile(List.of());
      verify(qualityGateServiceMock, never()).initPredefinedQualityGates();
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
    )
    void shouldNotCatchUnreadableGateFile_andWriteNoGates() {
      doReturn(true).when(gateOwnershipServiceMock).isFileOwned();
      doThrow(
        new QualityGateFileException("file:/gates.yaml", "it does not exist")
      )
        .when(qualityGateFileReaderMock)
        .readQualityGateFile();

      assertThatThrownBy(() -> fixture.run()).isInstanceOf(
        QualityGateFileException.class
      );

      verify(
        openApiCoverageConfigurationServiceMock
      ).initOpenApiCoverageCriteria();
      verify(qualityGateServiceMock, never()).initPredefinedQualityGates();
      verify(qualityGateServiceMock, never()).applyQualityGateFile(anyList());
    }
  }
}
