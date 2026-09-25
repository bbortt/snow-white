/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DBMigrationUnitTest {

  private static final Map<String, String> MIGRATIONS_AND_HASHES = Map.of(
    "V2025_06_29__init.sql",
    "17e9d29e14dfbe89a0f2fb297cb06fe43e03db1bba726b8f15b7df4cd1574523",
    "V2026_09_18__quality_gate_report_min_coverage_percentage.sql",
    "708f97a2aa148279c4ef15d6373bcfbad87aad6ff7a95a924b28d62ee8d8969e",
    "V2026_09_24__api_test_result_findings.sql",
    "a6a0cb2deaa238d63f6428ad044350670c483f5c43eee610bfa884a55d470f7b",
    "V2026_09_25__quality_gate_report_min_coverage_percentage_bounds.sql",
    "d1036e7ad0f737388b5e1c403cd9acf60281cded0458e9d6e59fab59349f93c1"
  );

  @Test
  void databaseMigrationsAreImmutable()
    throws IOException, NoSuchAlgorithmException {
    for (Map.Entry<String, String> entry : MIGRATIONS_AND_HASHES.entrySet()) {
      String fileName = entry.getKey();
      String expectedHash = entry.getValue();

      URL resource = getClass()
        .getClassLoader()
        .getResource("db/migration/" + fileName);
      assertThat(resource)
        .as("Migration file not found on classpath: %s", fileName)
        .isNotNull();

      byte[] content;
      try (InputStream stream = resource.openStream()) {
        content = stream.readAllBytes();
      }

      String actualHash = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(content)
      );
      assertThat(actualHash)
        .as(
          "Hash mismatch for migration file '%s' — do not modify existing migrations",
          fileName
        )
        .isEqualTo(expectedHash);
    }
  }

  @Test
  void noUntrackedMigrationShouldExist()
    throws IOException, URISyntaxException {
    URL migrationDir = getClass().getClassLoader().getResource("db/migration");
    assertThat(migrationDir)
      .as("db/migration directory not found on classpath")
      .isNotNull();

    try (var paths = Files.list(Path.of(migrationDir.toURI()))) {
      paths
        .map(Path::getFileName)
        .map(Path::toString)
        .filter(name -> name.matches("V.*\\.sql"))
        .forEach(name ->
          assertThat(MIGRATIONS_AND_HASHES)
            .as(
              "Untracked migration found: '%s' — add it to MIGRATIONS_AND_HASHES",
              name
            )
            .containsKey(name)
        );
    }
  }
}
