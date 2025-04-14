package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.IntegrationTest;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class QualityGateResourceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/quality-gates";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Test
  void createQualityGateConfig() throws Exception {
    var name = generateUniqueName("createQualityGateConfig");
    var qualityGateConfig = QualityGateConfig.builder().name(name).build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(qualityGateConfig))
      )
      .andExpect(status().isCreated())
      .andExpect(
        header()
          .string("location", format("/api/rest/v1/quality-gates/%s", name))
      )
      .andExpect(
        content().json(objectMapper.writeValueAsString(qualityGateConfig))
      );

    assertThat(qualityGateConfigurationRepository.findById(name)).isNotEmpty();
  }

  @Test
  void createQualityGateConfig_withoutRequiredName() throws Exception {
    var qualityGateConfig = QualityGateConfig.builder().build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(qualityGateConfig))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void deleteQualityGateConfig() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "deleteQualityGateConfig"
    );

    mockMvc
      .perform(
        delete(ENTITY_API_URL + "/" + qualityGateConfiguration.getName())
      )
      .andExpect(status().isNoContent());

    assertThat(
      qualityGateConfigurationRepository.findById(
        qualityGateConfiguration.getName()
      )
    ).isEmpty();
  }

  @Test
  void findAllQualityGateConfigs() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "findAllQualityGateConfigs"
    );

    mockMvc
      .perform(get(ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.names.length()").value(1))
      .andExpect(
        jsonPath("$.names[0]").value(qualityGateConfiguration.getName())
      );
  }

  @Test
  void findSingleQualityGateConfigByName() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "findSingleQualityGateConfigByName"
    )
      .withDescription("This is a complete Quality-Gate Configuration.")
      .withOpenApiCoverageConfiguration(
        OpenApiCoverageConfiguration.builder()
          .name(PATH_COVERAGE.name())
          .build()
      );
    qualityGateConfiguration = qualityGateConfigurationRepository.save(
      qualityGateConfiguration
    );

    mockMvc
      .perform(get(ENTITY_API_URL + "/" + qualityGateConfiguration.getName()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(qualityGateConfiguration.getName()))
      .andExpect(
        jsonPath("$.description").value(
          qualityGateConfiguration.getDescription()
        )
      )
      .andExpect(jsonPath("$.openapiCriteria").value(hasSize(1)))
      .andExpect(
        jsonPath("$.openapiCriteria[0]").value(is(PATH_COVERAGE.name()))
      );
  }

  @Test
  void updateQualityGateConfig() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "updateQualityGateConfig"
    ).withDescription("I just added this!");

    mockMvc
      .perform(
        put(ENTITY_API_URL + "/" + qualityGateConfiguration.getName())
          .contentType(APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(qualityGateConfiguration))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(qualityGateConfiguration.getName()))
      .andExpect(
        jsonPath("$.description").value(
          qualityGateConfiguration.getDescription()
        )
      );

    assertThat(
      qualityGateConfigurationRepository.findById(
        qualityGateConfiguration.getName()
      )
    )
      .isNotEmpty()
      .get()
      .extracting(QualityGateConfiguration::getDescription)
      .isEqualTo(qualityGateConfiguration.getDescription());
  }

  private QualityGateConfiguration createAndSaveQualityGateConfig(
    String findAllQualityGateConfigs
  ) {
    var name = generateUniqueName(findAllQualityGateConfigs);
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name(name)
      .build();
    return qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }

  private String generateUniqueName(String suffix) {
    return getClass().getSimpleName() + "_" + suffix;
  }
}
