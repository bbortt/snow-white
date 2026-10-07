/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractQualityGateApiIT;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Transactional
@AutoConfigureMockMvc
class QualityGateResourceIT extends AbstractQualityGateApiIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/quality-gates";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{qualityGateName}";

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private QualityGateConfigurationMapper qualityGateConfigurationMapper;

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

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
          .content(jsonMapper.writeValueAsString(qualityGateConfig))
      )
      .andExpect(status().isCreated())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(
        header().string(
          "location",
          format("/api/rest/v1/quality-gates/%s", name)
        )
      )
      .andExpect(
        content().json(jsonMapper.writeValueAsString(qualityGateConfig))
      );

    assertThat(qualityGateConfigurationRepository.findByName(name))
      .isNotEmpty()
      .map(QualityGateConfiguration::getName)
      .get()
      .isEqualTo(name);

    qualityGateConfigurationRepository.deleteByName(name);
  }

  @Test
  void createQualityGateConfig_withAttachedOpenAPI() throws Exception {
    var name = generateUniqueName("createQualityGateConfig");
    var qualityGateConfig = QualityGateConfig.builder()
      .name(name)
      .openApiCoverageCriteria(singletonList(PATH_COVERAGE.name()))
      .build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(qualityGateConfig))
      )
      .andExpect(status().isCreated())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(
        header().string(
          "location",
          format("/api/rest/v1/quality-gates/%s", name)
        )
      )
      .andExpect(
        content().json(jsonMapper.writeValueAsString(qualityGateConfig))
      );

    assertThat(qualityGateConfigurationRepository.findByName(name))
      .isNotEmpty()
      .map(QualityGateConfiguration::getOpenApiCoverageConfigurations)
      .get()
      .asInstanceOf(SET)
      .hasSize(1)
      .first()
      .asInstanceOf(type(QualityGateOpenApiCoverageMapping.class))
      .satisfies(qualityGateOpenApiCoverageMapping ->
        assertThat(
          qualityGateOpenApiCoverageMapping.getOpenApiCoverageConfiguration()
        )
          .extracting(OpenApiCoverageConfiguration::getName)
          .isEqualTo(PATH_COVERAGE.name())
      );

    qualityGateConfigurationRepository.deleteByName(name);
  }

  @Test
  void createQualityGateConfig_whereMinCoveragePercentageIsBelow80Percent()
    throws Exception {
    var name = generateUniqueName("createQualityGateConfig");
    var qualityGateConfig = QualityGateConfig.builder()
      .name(name)
      .minCoveragePercentage(79)
      .build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(qualityGateConfig))
      )
      .andExpect(status().isBadRequest());

    assertThat(qualityGateConfigurationRepository.findByName(name)).isEmpty();
  }

  @Test
  void createQualityGateConfig_withoutRequiredName() throws Exception {
    var qualityGateConfig = QualityGateConfig.builder().build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(qualityGateConfig))
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
      .perform(get(ENTITY_API_URL).param("sort", "name,asc"))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "4"))
      .andExpect(jsonPath("$.length()").value(4))
      .andExpect(
        jsonPath("$[*].name").value(
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
      .perform(get(ENTITY_API_URL).param("sort", "isPredefined,desc"))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "5"))
      .andExpect(jsonPath("$.length()").value(5))
      .andExpect(
        jsonPath("$[4].name").value(qualityGateConfiguration.getName())
      );
  }

  /**
   * Without a {@code sort} the listing still orders by name, so a caller that expresses no
   * preference gets the documented default rather than whatever the database returns.
   *
   * <p>Asserted on the relative position of two configurations sharing a name prefix, so the
   * expectation does not depend on where the database's collation puts the predefined gates.
   */
  @Test
  void findAllQualityGateConfigsWithoutSortOrdersByName() throws Exception {
    var last = createAndSaveQualityGateConfig("zzz-sorts-last");
    var first = createAndSaveQualityGateConfig("aaa-sorts-first");

    var names = jsonMapper
      .readTree(
        mockMvc
          .perform(get(ENTITY_API_URL))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString()
      )
      .valueStream()
      .map(node -> node.get("name").asString())
      .toList();

    assertThat(names).containsSubsequence(first.getName(), last.getName());
  }

  /**
   * A property this listing does not publish — or a value that does not parse — is the caller's
   * mistake. Unmapped it reached the default {@code 500}.
   */
  @ParameterizedTest
  @ValueSource(
    strings = { "description,asc", "createdAt,desc", "name", "name,sideways" }
  )
  void findAllQualityGateConfigsRejectsASortItDoesNotPublish(String sort)
    throws Exception {
    mockMvc
      .perform(get(ENTITY_API_URL).param("sort", sort))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("Bad Request"))
      .andExpect(
        jsonPath("$.message").value(
          containsString("one of: isPredefined, minCoveragePercentage, name")
        )
      );
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
      .andExpect(jsonPath("$.openApiCoverageCriteria").value(hasSize(1)))
      .andExpect(
        jsonPath("$.openApiCoverageCriteria[0]").value(is(PATH_COVERAGE.name()))
      );
  }

  @Test
  @VerifiesCon(ConTraceables.CON_010_REST_RESPONSES_NEVER_CARRY_NULL)
  void findSingleQualityGateConfigByName_withoutDescription_omitsIt()
    throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "withoutDescription"
    );

    var responseJson = jsonMapper.readTree(
      mockMvc
        .perform(get(SINGLE_ENTITY_API_URL, qualityGateConfiguration.getName()))
        .andExpect(status().isOk())
        .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        .andReturn()
        .getResponse()
        .getContentAsString()
    );

    assertThat(responseJson.get("name").asString()).isEqualTo(
      qualityGateConfiguration.getName()
    );
    // Read from the raw body: jsonPath's doesNotExist() would accept an explicit null as well
    assertThat(responseJson.has("description")).isFalse();
  }

  @Test
  void updateQualityGateConfig() throws Exception {
    var qualityGateConfiguration = createAndSaveQualityGateConfig(
      "updateQualityGateConfig"
    ).withDescription("I just added this!");

    var qualityGateConfig = qualityGateConfigurationMapper
      .toDto(qualityGateConfiguration)
      .openApiCoverageCriteria(singletonList(PATH_COVERAGE.name()));

    mockMvc
      .perform(
        put(SINGLE_ENTITY_API_URL, qualityGateConfiguration.getName())
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(qualityGateConfig))
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
  }

  private QualityGateConfiguration createAndSaveQualityGateConfig(
    String suffix
  ) {
    var name = generateUniqueName(suffix);
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name(name)
      .build();

    return qualityGateConfigurationRepository.saveAndFlush(
      qualityGateConfiguration
    );
  }

  private String generateUniqueName(String suffix) {
    return getClass().getSimpleName() + "_" + suffix;
  }
}
