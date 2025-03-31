package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource.QualityGateResource.QUALITY_GATE_CREATED_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.CreateQualityGate201Response;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith({ MockitoExtension.class })
class QualityGateResourceTest {

  @Mock
  private QualityGateConfigurationMapper qualityGateConfigurationMapperMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  private QualityGateResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateResource(
      qualityGateConfigurationMapperMock,
      qualityGateServiceMock
    );
  }

  @Test
  void qualityGateCreatedMessageConstant_shouldBeCorrect() {
    assertThat(QUALITY_GATE_CREATED_MESSAGE).isEqualTo(
      "Quality-Gate configuration created successfully"
    );
  }

  @Nested
  class CreateQualityGate {

    QualityGateConfig qualityGateConfig;

    @BeforeEach
    void beforeEachSetup() {
      qualityGateConfig = QualityGateConfig.builder()
        .name("TestQualityGate")
        .build();
    }

    @Test
    void shouldReturnCreatedResponse()
      throws QualityGateService.ConfigurationNameAlreadyExistsException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .fromDto(qualityGateConfig);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(CreateQualityGate201Response.class))
              .satisfies(
                q -> assertThat(q.getName()).isEqualTo("TestQualityGate"),
                q ->
                  assertThat(q.getMessage()).isEqualTo(
                    QUALITY_GATE_CREATED_MESSAGE
                  )
              )
        );

      verify(qualityGateServiceMock).persist(qualityGateConfiguration);
    }

    @Test
    void shouldReturnConflictResponse_whenConfigurationAlreadyExists()
      throws QualityGateService.ConfigurationNameAlreadyExistsException {
      var qualityGateConfiguration = new QualityGateConfiguration();

      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .fromDto(qualityGateConfig);
      doThrow(new QualityGateService.ConfigurationNameAlreadyExistsException())
        .when(qualityGateServiceMock)
        .persist(qualityGateConfiguration);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(CONFLICT),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Conflict"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "Quality-Gate configuration with name 'TestQualityGate' already exists"
                  )
              )
        );
    }

    @Test
    void createdResponseShouldContainCorrectLocationUri() {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .fromDto(qualityGateConfig);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response.getHeaders().getLocation()).isEqualTo(
        URI.create("/v1/quality-gates/TestQualityGate")
      );
    }
  }

  @Nested
  class GetQualityGateByName {

    @Test
    void shouldReturnConfiguration()
      throws QualityGateService.ConfigurationDoesNotExistException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateServiceMock)
        .findByName(qualityGateConfiguration.getName());

      var qualityGateConfig = new QualityGateConfig();
      doReturn(qualityGateConfig)
        .when(qualityGateConfigurationMapperMock)
        .toDto(qualityGateConfiguration);

      var response = fixture.getQualityGateByName(
        qualityGateConfiguration.getName()
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEqualTo(qualityGateConfig)
        );
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationDoesNotExists()
      throws QualityGateService.ConfigurationDoesNotExistException {
      var name = "NonExistingConfiguration";
      doThrow(new QualityGateService.ConfigurationDoesNotExistException())
        .when(qualityGateServiceMock)
        .findByName(name);

      var response = fixture.getQualityGateByName(name);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Not Found"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "Quality-Gate configuration with name 'NonExistingConfiguration' does not exist"
                  )
              )
        );
    }
  }
}
