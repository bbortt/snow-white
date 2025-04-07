package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class DatabaseInitializerTest {

  @Mock
  private OpenApiCoverageConfigurationService openApiCoverageConfigurationServiceMock;

  private DatabaseInitializer fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DatabaseInitializer(openApiCoverageConfigurationServiceMock);
  }

  @Nested
  class Run {

    @Test
    void shouldInitiateDatabaseInitialization() {
      fixture.run();

      verify(openApiCoverageConfigurationServiceMock).initOpenApiCriteria();
    }
  }
}
