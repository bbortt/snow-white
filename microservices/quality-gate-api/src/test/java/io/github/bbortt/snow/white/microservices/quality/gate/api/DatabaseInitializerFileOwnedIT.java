/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static java.nio.file.Files.delete;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.init.DatabaseInitializer;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.QualityGateFileException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DatabaseInitializerFileOwnedIT extends AbstractFileOwnedQualityGateApiIT {

  @Autowired
  private DatabaseInitializer databaseInitializer;

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Autowired
  private QualityGateService qualityGateService;

  @BeforeEach
  @AfterEach
  void resetSharedState() {
    writeGateFile(INITIAL_GATE_FILE_CONTENT);

    qualityGateConfigurationRepository.deleteAll();
    qualityGateConfigurationRepository.flush();
  }

  @Test
  @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  @VerifiesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  void startupMakesTheGateSetExactlyTheFile() {
    qualityGateService.applyQualityGateFile(
      List.of(
        gate("gate-a", "as the database has it", 100, "PATH_COVERAGE"),
        gate("gate-b", "not in the file", 100)
      )
    );

    writeGateFile(
      """
      - name: gate-a
        description: as the file defines it
        minCoveragePercentage: 90
        openApiCoverageCriteria:
          - HTTP_METHOD_COVERAGE
      - name: gate-c
        minCoveragePercentage: 100
        openApiCoverageCriteria:
          - PATH_COVERAGE
          - PARAMETER_COVERAGE
      """
    );

    databaseInitializer.run();

    assertThat(snapshot()).containsOnly(
      Map.entry(
        "gate-a",
        new GateView(
          "as the file defines it",
          90,
          false,
          Set.of("HTTP_METHOD_COVERAGE")
        )
      ),
      Map.entry(
        "gate-c",
        new GateView(
          null,
          100,
          false,
          Set.of("PATH_COVERAGE", "PARAMETER_COVERAGE")
        )
      )
    );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  @VerifiesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  @VerifiesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
  void startupAgainstTheSameFileChangesNothing() {
    writeGateFile(
      """
      - name: gate-a
        description: first
        minCoveragePercentage: 100
        openApiCoverageCriteria:
          - PATH_COVERAGE
      - name: gate-b
        minCoveragePercentage: 85
      """
    );

    databaseInitializer.run();
    var afterFirstStartup = snapshot();

    databaseInitializer.run();

    assertThat(afterFirstStartup).hasSize(2);
    assertThat(snapshot()).isEqualTo(afterFirstStartup);
    // Criteria seeding runs first, every time, and is unaffected by the file.
    assertThat(openApiCoverageConfigurationRepository.count()).isEqualTo(
      OpenApiCoverageCriteria.values().length
    );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  void startupWithAnUnknownCriterionFailsAndWritesNothing() {
    qualityGateService.applyQualityGateFile(
      List.of(
        gate("gate-a", "untouched", 100, "PATH_COVERAGE"),
        gate("gate-b", "untouched", 100)
      )
    );
    var before = snapshot();

    writeGateFile(
      """
      - name: gate-a
        minCoveragePercentage: 100
        openApiCoverageCriteria:
          - NO_SUCH_CRITERION
      - name: gate-c
        minCoveragePercentage: 100
      """
    );

    assertThatThrownBy(() -> databaseInitializer.run()).hasMessageContaining(
      "NO_SUCH_CRITERION"
    );

    assertThat(snapshot()).isEqualTo(before);
  }

  @Test
  @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  void startupWithAConfiguredButAbsentFileFailsAndDeletesNoGate() throws IOException {
    qualityGateService.applyQualityGateFile(
      List.of(gate("gate-a", "untouched", 100), gate("gate-b", null, 100))
    );
    var before = snapshot();

    delete(GATE_FILE);

    assertThatThrownBy(() -> databaseInitializer.run()).isInstanceOf(
      QualityGateFileException.class
    );

    assertThat(snapshot()).isEqualTo(before);
  }

  @Test
  @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  @VerifiesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  void startupWithNoGateInTheFileLeavesTheInstanceWithNone() {
    qualityGateService.applyQualityGateFile(List.of(gate("gate-a", null, 100)));

    writeGateFile("");

    databaseInitializer.run();

    assertThat(snapshot()).isEmpty();
  }

  @Test
  @VerifiesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  @VerifiesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
  void startupDropsSeededPredefinedGatesTheFileDoesNotNameAndTheyStayGone() {
    qualityGateService.initPredefinedQualityGates();
    assertThat(snapshot())
      .containsOnlyKeys("full-feature", "basic-coverage", "minimal", "dry-run")
      .allSatisfy((name, gate) -> assertThat(gate.predefined()).isTrue());

    writeGateFile("- name: gate-a\n  minCoveragePercentage: 100\n");

    databaseInitializer.run();
    assertThat(snapshot()).containsOnlyKeys("gate-a");

    databaseInitializer.run();
    assertThat(snapshot()).containsOnlyKeys("gate-a");
  }

  private static QualityGateConfig gate(
    String name,
    String description,
    int minCoveragePercentage,
    String... criteria
  ) {
    return QualityGateConfig.builder()
      .name(name)
      .description(description)
      .minCoveragePercentage(minCoveragePercentage)
      .openApiCoverageCriteria(List.of(criteria))
      .build();
  }

  private Map<String, GateView> snapshot() {
    return new TransactionTemplate(transactionManager).execute(status ->
      qualityGateConfigurationRepository
        .findAll()
        .stream()
        .collect(
          toMap(
            QualityGateConfiguration::getName,
            DatabaseInitializerFileOwnedIT::view
          )
        )
    );
  }

  private static GateView view(QualityGateConfiguration gate) {
    return new GateView(
      gate.getDescription(),
      gate.getMinCoveragePercentage(),
      gate.getIsPredefined(),
      gate
        .getOpenApiCoverageConfigurations()
        .stream()
        .map(mapping -> mapping.getOpenApiCoverageConfiguration().getName())
        .collect(toSet())
    );
  }

  private record GateView(
    String description,
    int minCoveragePercentage,
    boolean predefined,
    Set<String> criteria
  ) {}
}
