/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

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

class DBMigrationTest {

  private static final Map<String, String> MIGRATIONS_AND_HASHES = Map.of(
    "V2025_06_29__init.sql",
    "3e4aaf77b0ed3ebf8204acdfbf4c8f8b7a21d1f59a9129bba9a27824c9a84224"
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
