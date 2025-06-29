package io.github.bbortt.snow.white.microservices.quality.gate.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(classes = { Main.class })
public abstract class AbstractQualityGateApiIT {

  static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
    (PostgreSQLContainer<?>) new PostgreSQLContainer(
      "postgres:17.5-alpine"
    ).withExposedPorts(5432);

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
