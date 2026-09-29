/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.commons.event.dto.FindingEvidence.MAX_TEST_CASE_NAME_BYTES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Holds this service's storage against the bound the coverage stream applies.
 * <p>
 * {@code FindingEvidence#MAX_TEST_CASE_NAME_BYTES} is enforced in openapi-coverage-stream, one
 * deployable away, while the column and the published schema that have to hold what it admits live
 * here. Nothing but this test keeps the two in step, and the failure it guards against is a name
 * the stream lets through, failing the insert and discarding a whole report.
 * <p>
 * Both assertions are {@code >=} rather than {@code ==} deliberately - widening storage alone is
 * always safe, and only narrowing it below the bound is the bug.
 */
class FindingEvidenceTestIdentityBoundUnitTest {

  private static final Pattern TEST_CASE_NAME_COLUMN = Pattern.compile(
    "test_case_name\\s+(?:TYPE\\s+)?VARCHAR\\((\\d+)\\)",
    Pattern.CASE_INSENSITIVE
  );

  private static final Pattern TEST_CASE_NAME_MAX_LENGTH = Pattern.compile(
    "testCaseName:.*?maxLength:\\s*(\\d+)",
    Pattern.DOTALL
  );

  @Test
  @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  void columnHoldsEveryNameTheCoverageStreamAdmits()
    throws IOException, URISyntaxException {
    var width = lastDeclaredColumnWidth();

    assertThat(width)
      .as(
        "finding_evidence.test_case_name is VARCHAR(%d), narrower than the %d UTF-8 bytes openapi-coverage-stream admits — a name between the two fails the insert",
        width,
        MAX_TEST_CASE_NAME_BYTES
      )
      .isGreaterThanOrEqualTo(MAX_TEST_CASE_NAME_BYTES);
  }

  @Test
  @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  void publishedSchemaHoldsEveryNameTheCoverageStreamAdmits()
    throws IOException {
    var schema = readClasspathResource(
      "openapi/components/FindingEvidence.yml"
    );
    var matcher = TEST_CASE_NAME_MAX_LENGTH.matcher(schema);

    assertThat(matcher.find())
      .as("FindingEvidence.yml declares no maxLength for testCaseName")
      .isTrue();

    var maxLength = Integer.parseInt(matcher.group(1));
    assertThat(maxLength)
      .as(
        "FindingEvidence.yml bounds testCaseName at %d, below the %d UTF-8 bytes openapi-coverage-stream admits — a generated client would reject a report this service stored",
        maxLength,
        MAX_TEST_CASE_NAME_BYTES
      )
      .isGreaterThanOrEqualTo(MAX_TEST_CASE_NAME_BYTES);
  }

  /**
   * The width the latest migration to touch the column leaves it at, since an earlier migration's
   * declaration is superseded rather than wrong.
   */
  private int lastDeclaredColumnWidth() throws IOException, URISyntaxException {
    URL migrationDir = getClass().getClassLoader().getResource("db/migration");
    assertThat(migrationDir)
      .as("db/migration directory not found on classpath")
      .isNotNull();

    Integer width = null;
    try (var paths = Files.list(Path.of(migrationDir.toURI()))) {
      for (var migration : paths
        .filter(path -> path.getFileName().toString().matches("V.*\\.sql"))
        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
        .toList()) {
        var matcher = TEST_CASE_NAME_COLUMN.matcher(
          Files.readString(migration, UTF_8)
        );
        while (matcher.find()) {
          width = Integer.parseInt(matcher.group(1));
        }
      }
    }

    assertThat(width)
      .as("no migration declares a width for finding_evidence.test_case_name")
      .isNotNull();

    return width;
  }

  private String readClasspathResource(String name) throws IOException {
    URL resource = getClass().getClassLoader().getResource(name);
    assertThat(resource)
      .as("Resource not found on classpath: %s", name)
      .isNotNull();

    try (var stream = resource.openStream()) {
      return new String(stream.readAllBytes(), UTF_8);
    }
  }
}
