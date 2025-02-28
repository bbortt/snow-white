package io.github.bbortt.snow.white.microservices.quality.gate.api.rest;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.rest.QualityGatesResource.QUALITY_GATE_CREATED_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.CreateQualityGate201Response;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfigCriteria;
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
class QualityGatesResourceTest {

  @Mock
  private QualityGateService qualityGateServiceMock;

  private QualityGatesResource fixture;

  @BeforeEach
  void setUp() {
    fixture = new QualityGatesResource(qualityGateServiceMock);
  }

  @Test
  void qualityGateCreatedMessageConstant_shouldBeCorrect() {
    assertThat(QUALITY_GATE_CREATED_MESSAGE).isEqualTo(
      "Quality-Gate configuration created successfully"
    );
  }

  @Nested
  class CreateQualityGate {

    @Test
    void shouldReturnCreatedResponse()
      throws QualityGateService.ConfigurationNameAlreadyExistsException {
      var qualityGateConfig = QualityGateConfig.builder()
        .name("TestQualityGate")
        .criteria(new QualityGateConfigCriteria())
        .build();

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

      verify(qualityGateServiceMock).persist(
        any(QualityGateConfiguration.class)
      );
    }

    @Test
    void shouldReturnConflictResponse_whenConfigurationAlreadyExists()
      throws QualityGateService.ConfigurationNameAlreadyExistsException {
      var qualityGateConfig = QualityGateConfig.builder()
        .name("ExistingQualityGate")
        .criteria(new QualityGateConfigCriteria())
        .build();

      doThrow(new QualityGateService.ConfigurationNameAlreadyExistsException())
        .when(qualityGateServiceMock)
        .persist(any(QualityGateConfiguration.class));

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(CONFLICT),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(
                type(
                  io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.Error.class
                )
              )
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Conflict"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "Quality-Gate with name 'ExistingQualityGate' already exists"
                  )
              )
        );
    }

    @Test
    void createdResponseShouldContainCorrectLocationUri() {
      var qualityGateConfig = QualityGateConfig.builder()
        .name("TestQualityGate")
        .criteria(new QualityGateConfigCriteria())
        .build();

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
      var qualityGateConfig = QualityGateConfiguration.builder()
        .name("TestQualityGate")
        .build();

      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findByName(qualityGateConfig.getName());

      var response = fixture.getQualityGateByName(qualityGateConfig.getName());

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isInstanceOf(QualityGateConfig.class)
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
              .asInstanceOf(
                type(
                  io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.Error.class
                )
              )
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Not Found"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "Quality-Gate with name 'NonExistingConfiguration' does not exist"
                  )
              )
        );
    }
  }
}
