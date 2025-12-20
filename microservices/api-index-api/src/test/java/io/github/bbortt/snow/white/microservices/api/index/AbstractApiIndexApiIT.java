/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.index.public-api-gateway-url=http://localhost:9080",
  }
)
public abstract class AbstractApiIndexApiIT {

  static final PostgreSQLContainer POSTGRESQL_CONTAINER =
    new PostgreSQLContainer("postgres:18.1-alpine").withExposedPorts(5432);

  static {
    POSTGRESQL_CONTAINER.start();
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add(
      "spring.datasource.username",
      POSTGRESQL_CONTAINER::getUsername
    );
    registry.add(
      "spring.datasource.password",
      POSTGRESQL_CONTAINER::getPassword
    );
  }
}
