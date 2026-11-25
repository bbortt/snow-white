/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.QualityGateFileException;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.error.YAMLException;

class QualityGateFileReaderUnitTest {

  private static final String YAML_GATES = """
  - name: strict
    description: Everything, all the way
    minCoveragePercentage: 95
    openApiCoverageCriteria:
      - PATH_COVERAGE
      - RESPONSE_CODE_COVERAGE
  - name: lenient
    isPredefined: false
    minCoveragePercentage: 80
    openApiCoverageCriteria: []
  """;

  private static final String JSON_GATES = """
  [
    {
      "name": "strict",
      "description": "Everything, all the way",
      "minCoveragePercentage": 95,
      "openApiCoverageCriteria": ["PATH_COVERAGE", "RESPONSE_CODE_COVERAGE"]
    },
    {
      "name": "lenient",
      "isPredefined": false,
      "minCoveragePercentage": 80,
      "openApiCoverageCriteria": []
    }
  ]
  """;

  @TempDir
  private Path tempDir;

  private QualityGateApiProperties qualityGateApiProperties;

  private QualityGateFileReader fixture;

  @BeforeEach
  void beforeEachSetup() {
    qualityGateApiProperties = new QualityGateApiProperties();

    fixture = new QualityGateFileReader(
      qualityGateApiProperties,
      new DefaultResourceLoader(),
      buildDefaultValidatorFactory().getValidator()
    );
  }

  private String configureGateFile(
    String name,
    String content
  ) throws IOException {
    var file = tempDir.resolve(name);
    writeString(file, content);

    var location = file.toUri().toString();
    qualityGateApiProperties.setGateFile(location);
    return location;
  }

  @Nested
  class ReadQualityGateFileTest {

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldParseYaml() throws IOException {
      configureGateFile("gates.yaml", YAML_GATES);

      var gates = fixture.readQualityGateFile();

      assertThat(gates).hasSize(2);
      assertThat(gates.get(0)).satisfies(
        g -> assertThat(g.getName()).isEqualTo("strict"),
        g ->
          assertThat(g.getDescription()).isEqualTo("Everything, all the way"),
        g -> assertThat(g.getMinCoveragePercentage()).isEqualTo(95),
        g ->
          assertThat(g.getOpenApiCoverageCriteria()).containsExactly(
            "PATH_COVERAGE",
            "RESPONSE_CODE_COVERAGE"
          )
      );
      assertThat(gates.get(1)).satisfies(
        g -> assertThat(g.getName()).isEqualTo("lenient"),
        g -> assertThat(g.getDescription()).isNull(),
        g -> assertThat(g.getIsPredefined()).isFalse(),
        g -> assertThat(g.getMinCoveragePercentage()).isEqualTo(80),
        g -> assertThat(g.getOpenApiCoverageCriteria()).isEmpty()
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldParseJsonIntoTheSameGateSetAsYaml() throws IOException {
      configureGateFile("gates.yaml", YAML_GATES);
      var fromYaml = fixture.readQualityGateFile();

      configureGateFile("gates.json", JSON_GATES);
      var fromJson = fixture.readQualityGateFile();

      assertThat(fromJson).hasSize(2).isEqualTo(fromYaml);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseConfiguredButAbsentFile_insteadOfReadingItAsEmpty() {
      var location = tempDir.resolve("not-mounted.yaml").toUri().toString();
      qualityGateApiProperties.setGateFile(location);

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessage(
          "Quality-Gate file '%s' cannot be used: it does not exist!".formatted(
            location
          )
        )
        .hasNoCause();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseExistingButUnreadableFile() throws IOException {
      var location = "file:/etc/gates/gates.yaml";
      qualityGateApiProperties.setGateFile(location);

      var cause = new IOException("permission denied");
      var unreadable = mock(Resource.class);
      doReturn(true).when(unreadable).exists();
      doThrow(cause).when(unreadable).getInputStream();

      var resourceLoader = mock(ResourceLoader.class);
      doReturn(unreadable).when(resourceLoader).getResource(location);

      var unreadableFixture = new QualityGateFileReader(
        qualityGateApiProperties,
        resourceLoader,
        buildDefaultValidatorFactory().getValidator()
      );

      assertThatThrownBy(unreadableFixture::readQualityGateFile)
        .isInstanceOf(QualityGateFileException.class)
        .hasMessage(
          "Quality-Gate file '%s' cannot be used: it could not be parsed as JSON or YAML!".formatted(
            location
          )
        )
        .hasCause(cause);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseSyntacticallyInvalidFile() throws IOException {
      var location = configureGateFile("broken.yaml", "- name: [unclosed\n");

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessage(
          "Quality-Gate file '%s' cannot be used: it could not be parsed as JSON or YAML!".formatted(
            location
          )
        )
        .hasCauseInstanceOf(YAMLException.class);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseDocumentThatIsNotAList() throws IOException {
      var location = configureGateFile(
        "map.yaml",
        "name: strict\nminCoveragePercentage: 95\n"
      );

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessage(
          "Quality-Gate file '%s' cannot be used: it is not a list of Quality-Gate definitions!".formatted(
            location
          )
        )
        .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseMisspeltKey_insteadOfLeavingTheFieldAtItsDefault() throws IOException {
      configureGateFile(
        "typo.yaml",
        """
        - name: strict
          minCoveragePercentage: 95
          openApiCoverageCriterias:
            - PATH_COVERAGE
        """
      );

      assertThatThrownBy(() -> fixture.readQualityGateFile()).isInstanceOf(
        QualityGateFileException.class
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldAcceptEmptyFileAsEmptyGateSet() throws IOException {
      configureGateFile("empty.yaml", "");

      assertThat(fixture.readQualityGateFile()).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldAcceptCommentOnlyFileAsEmptyGateSet() throws IOException {
      configureGateFile("comments.yaml", "# no gates on this instance\n");

      assertThat(fixture.readQualityGateFile()).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldAcceptEmptyListAsEmptyGateSet() throws IOException {
      configureGateFile("empty-list.json", "[]");

      assertThat(fixture.readQualityGateFile()).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseAListEntryThatDefinesNoGate() throws IOException {
      var location = configureGateFile("empty-entry.yaml", "- \n");

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessage(
          "Quality-Gate file '%s' cannot be used: it has a list entry that defines no Quality-Gate at all!".formatted(
            location
          )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseTwoGatesSharingAName() throws IOException {
      configureGateFile(
        "duplicate.yaml",
        """
        - name: twice
          minCoveragePercentage: 100
          openApiCoverageCriteria: []
        - name: twice
          minCoveragePercentage: 80
          openApiCoverageCriteria: []
        """
      );

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessageContaining(
          "it defines more than one Quality-Gate named twice"
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldClearWhatTheFileOmits() throws IOException {
      configureGateFile("sparse.yaml", "- name: states-only-its-name\n");

      assertThat(fixture.readQualityGateFile())
        .singleElement()
        .satisfies(
          qualityGateConfig ->
            assertThat(
              qualityGateConfig.getOpenApiCoverageCriteria()
            ).isEmpty(),
          qualityGateConfig ->
            assertThat(qualityGateConfig.getDescription()).isNull(),
          qualityGateConfig ->
            assertThat(qualityGateConfig.getMinCoveragePercentage()).isEqualTo(
              100
            ),
          qualityGateConfig ->
            assertThat(qualityGateConfig.getIsPredefined()).isFalse()
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldNeverReadAFileGateAsPredefined() throws IOException {
      configureGateFile(
        "claims-predefined.yaml",
        """
        - name: claims-to-be-predefined
          isPredefined: true
          minCoveragePercentage: 100
          openApiCoverageCriteria: []
        """
      );

      assertThat(fixture.readQualityGateFile())
        .singleElement()
        .satisfies(qualityGateConfig ->
          assertThat(qualityGateConfig.getIsPredefined()).isFalse()
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldRefuseAThresholdTheCreateEndpointWouldReject() throws IOException {
      configureGateFile(
        "too-low.yaml",
        """
        - name: barely-anything
          minCoveragePercentage: 5
          openApiCoverageCriteria: []
        """
      );

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessageContaining(
          "Quality-Gate 'barely-anything' minCoveragePercentage"
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
    void shouldReportEveryInvalidGate_notOnlyTheFirst() throws IOException {
      configureGateFile(
        "two-bad.yaml",
        """
        - name: first-bad
          minCoveragePercentage: 5
          openApiCoverageCriteria: []
        - name: second-bad
          minCoveragePercentage: 101
          openApiCoverageCriteria: []
        """
      );

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(QualityGateFileException.class)
        .hasMessageContaining("first-bad")
        .hasMessageContaining("second-bad");
    }

    @Test
    void shouldFailFast_whenNoFileIsConfigured() {
      qualityGateApiProperties.setGateFile(null);

      assertThatThrownBy(() -> fixture.readQualityGateFile())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Quality-Gate file location must be set to read it");
    }
  }
}
