/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
class QualityGateResourceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/quality-gates";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{qualityGateName}";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Test
  @Transactional
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
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(
        header()
          .string("location", format("/api/rest/v1/quality-gates/%s", name))
      )
      .andExpect(
        content().json(objectMapper.writeValueAsString(qualityGateConfig))
      );

    assertThat(
      qualityGateConfigurationRepository.findByName(name)
    ).isNotEmpty();

    qualityGateConfigurationRepository.deleteByName(name);
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
  @Transactional
  void deleteQualityGateConfig() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "deleteQualityGateConfig"
    );

    mockMvc
      .perform(
        delete(SINGLE_ENTITY_API_URL, qualityGateConfiguration.getName())
      )
      .andExpect(status().isNoContent());

    assertThat(
      qualityGateConfigurationRepository.existsByName(
        qualityGateConfiguration.getName()
      )
    ).isFalse();
  }

  @Test
  void findPredefinedQualityGateConfigs() throws Exception {
    mockMvc
      .perform(get(ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.names.length()").value(4))
      .andExpect(
        jsonPath("$.names").value(
          contains("basic-coverage", "dry-run", "full-feature", "minimal")
        )
      );
  }

  @Test
  void findAllQualityGateConfigs() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "findAllQualityGateConfigs"
    );

    mockMvc
      .perform(get(ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.names.length()").value(5))
      .andExpect(
        jsonPath("$.names[0]").value(qualityGateConfiguration.getName())
      );

    qualityGateConfigurationRepository.delete(qualityGateConfiguration);
  }

  @Test
  void findSingleQualityGateConfigByName() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "findSingleQualityGateConfigByName"
    )
      .withDescription("This is a complete Quality-Gate Configuration.")
      .withOpenApiCoverageConfiguration(
        openApiCoverageConfigurationRepository
          .findByName(PATH_COVERAGE.name())
          .orElseThrow(IllegalArgumentException::new)
      );
    qualityGateConfiguration = qualityGateConfigurationRepository.save(
      qualityGateConfiguration
    );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, qualityGateConfiguration.getName()))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
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

    qualityGateConfigurationRepository.delete(qualityGateConfiguration);
  }

  @Test
  void updateQualityGateConfig() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "updateQualityGateConfig"
    ).withDescription("I just added this!");

    mockMvc
      .perform(
        put(SINGLE_ENTITY_API_URL, qualityGateConfiguration.getName())
          .contentType(APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(qualityGateConfiguration))
      )
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.name").value(qualityGateConfiguration.getName()))
      .andExpect(
        jsonPath("$.description").value(
          qualityGateConfiguration.getDescription()
        )
      );

    assertThat(
      qualityGateConfigurationRepository.findByName(
        qualityGateConfiguration.getName()
      )
    )
      .isNotEmpty()
      .get()
      .extracting(QualityGateConfiguration::getDescription)
      .isEqualTo(qualityGateConfiguration.getDescription());

    qualityGateConfigurationRepository.delete(qualityGateConfiguration);
  }

  private QualityGateConfiguration createAndSaveQualityGateConfig(
    String suffix
  ) {
    var name = generateUniqueName(suffix);
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name(name)
      .build();

    return qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }

  private String generateUniqueName(String suffix) {
    return getClass().getSimpleName() + "_" + suffix;
  }
}
