/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.writeString;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * An instance whose gates are owned by a file. Every subclass shares one Spring context — the
 * property set is identical — and therefore one gate file, which is why a subclass that rewrites it
 * puts it back before the next test.
 *
 * <p>The ITs of this module share a single database, and a file-owned context deletes the predefined
 * gates the other contexts' tests expect to find. The class therefore re-seeds them once it is done.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractFileOwnedQualityGateApiIT
  extends AbstractQualityGateApiIT
{

  /** The one gate the file defines when the context starts. */
  protected static final String INITIAL_FILE_GATE = "initial-file-gate";

  protected static final String INITIAL_GATE_FILE_CONTENT =
    "- name: " + INITIAL_FILE_GATE + "\n  minCoveragePercentage: 100\n";

  protected static final Path GATE_FILE = createGateFile();

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Autowired
  private QualityGateService qualityGateService;

  @DynamicPropertySource
  static void gateFileProperty(DynamicPropertyRegistry registry) {
    registry.add("snow.white.quality.gate.api.gate-file", () ->
      GATE_FILE.toUri().toString()
    );
  }

  private static Path createGateFile() {
    try {
      var gateFile = createTempFile("quality-gates", ".yaml");
      gateFile.toFile().deleteOnExit();
      writeString(gateFile, INITIAL_GATE_FILE_CONTENT, UTF_8);
      return gateFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static void writeGateFile(String content) {
    try {
      writeString(GATE_FILE, content, UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @AfterAll
  void restoreWhatTheOtherContextsExpect() {
    writeGateFile(INITIAL_GATE_FILE_CONTENT);

    qualityGateConfigurationRepository.deleteAll();
    qualityGateConfigurationRepository.flush();
    qualityGateService.initPredefinedQualityGates();
  }
}
