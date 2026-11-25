/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.HEADER_X_QUALITY_GATE_OWNERSHIP;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE_EMPTY;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource.QualityGateResource.GATES_ARE_FILE_OWNED;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource.QualityGateResource.GATE_FILE_DEFINES_NO_GATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractFileOwnedQualityGateApiIT;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every test runs in a transaction that is rolled back, so the gate table is whatever {@link
 * #arrangeGates()} left in it, whatever the other ITs of this module's shared database did before.
 */
@Transactional
@AutoConfigureMockMvc
class QualityGateResourceFileOwnedIT extends AbstractFileOwnedQualityGateApiIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/quality-gates";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{qualityGateName}";

  private static final String FILE_GATE = "file-owned-gate";
  private static final String PREDEFINED_GATE = "file-owned-predefined-gate";
  private static final String UNKNOWN_GATE = "no-such-gate";

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private QualityGateConfigurationMapper qualityGateConfigurationMapper;

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @BeforeEach
  void arrangeGates() {
    qualityGateConfigurationRepository.deleteAll();
    qualityGateConfigurationRepository.flush();

    qualityGateConfigurationRepository.saveAndFlush(
      QualityGateConfiguration.builder()
        .name(FILE_GATE)
        .description("as the file defines it")
        .build()
    );
    qualityGateConfigurationRepository.saveAndFlush(
      QualityGateConfiguration.builder()
        .name(PREDEFINED_GATE)
        .isPredefined(true)
        .build()
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { FILE_GATE, UNKNOWN_GATE, PREDEFINED_GATE })
  @VerifiesSw(
    SwTraceables.SW_051_MANAGED_INSTANCE_REJECTS_CRUD_WRITES_WITH_CONFLICT
  )
  @VerifiesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  void everyGateWriteIsRefusedWithConflict(String name) throws Exception {
    var body = jsonMapper.writeValueAsString(
      QualityGateConfig.builder()
        .name(name)
        .description("changed through the API")
        .build()
    );

    mockMvc
      .perform(post(ENTITY_API_URL).contentType(APPLICATION_JSON).content(body))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value(GATES_ARE_FILE_OWNED));

    mockMvc
      .perform(
        put(SINGLE_ENTITY_API_URL, name)
          .contentType(APPLICATION_JSON)
          .content(body)
      )
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value(GATES_ARE_FILE_OWNED));

    mockMvc
      .perform(delete(SINGLE_ENTITY_API_URL, name))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value(GATES_ARE_FILE_OWNED));

    assertThat(qualityGateConfigurationRepository.findAll())
      .extracting(
        QualityGateConfiguration::getName,
        QualityGateConfiguration::getDescription
      )
      .containsExactlyInAnyOrder(
        tuple(FILE_GATE, "as the file defines it"),
        tuple(PREDEFINED_GATE, null)
      );
  }

  @Test
  @VerifiesSw(
    SwTraceables.SW_051_MANAGED_INSTANCE_REJECTS_CRUD_WRITES_WITH_CONFLICT
  )
  void readsAnswerAsWithTheModeOff() throws Exception {
    var expectedGate = jsonMapper.writeValueAsString(
      qualityGateConfigurationMapper.toDto(
        qualityGateConfigurationRepository.findByName(FILE_GATE).orElseThrow()
      )
    );

    mockMvc
      .perform(get(ENTITY_API_URL).param("sort", "name,asc"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(2)))
      .andExpect(jsonPath("$[0].name").value(FILE_GATE))
      .andExpect(jsonPath("$[1].name").value(PREDEFINED_GATE));

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, FILE_GATE))
      .andExpect(status().isOk())
      .andExpect(content().json(expectedGate, true));

    mockMvc
      .perform(get("/api/rest/v1/criteria/openapi"))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$.length()").value(OpenApiCoverageCriteria.values().length)
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
  void gateReadsPublishTheFileMode() throws Exception {
    mockMvc
      .perform(get(ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE)
      );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, FILE_GATE))
      .andExpect(status().isOk())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE)
      );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, UNKNOWN_GATE))
      .andExpect(status().isNotFound())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE)
      );

    // Produced by the exception handler, not by the resource: the reason the header is set in a filter.
    mockMvc
      .perform(get(ENTITY_API_URL).param("sort", "name,sideways"))
      .andExpect(status().isBadRequest())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE)
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
  void gateReadsPublishTheFileEmptyModeWhenTheFileDefinesNoGates() throws Exception {
    qualityGateConfigurationRepository.deleteAll();
    qualityGateConfigurationRepository.flush();

    mockMvc
      .perform(get(ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(0)))
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE_EMPTY)
      );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, UNKNOWN_GATE))
      .andExpect(status().isNotFound())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE_EMPTY)
      );

    mockMvc
      .perform(get(ENTITY_API_URL).param("sort", "name,sideways"))
      .andExpect(status().isBadRequest())
      .andExpect(
        header().string(HEADER_X_QUALITY_GATE_OWNERSHIP, OWNERSHIP_FILE_EMPTY)
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
  void unknownGateOnAnInstanceWhoseFileDefinesNoGatesNamesTheFile() throws Exception {
    qualityGateConfigurationRepository.deleteAll();
    qualityGateConfigurationRepository.flush();

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, UNKNOWN_GATE))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value(
          "Quality-Gate configuration '" +
            UNKNOWN_GATE +
            "' does not exist! " +
            GATE_FILE_DEFINES_NO_GATES
        )
      )
      .andExpect(
        jsonPath("$.message").value(endsWith(GATE_FILE_DEFINES_NO_GATES))
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
  void unknownGateOnAFileOwnedInstanceHoldingGatesIsAPlainDoesNotExist() throws Exception {
    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, UNKNOWN_GATE))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value(
          "Quality-Gate configuration '" + UNKNOWN_GATE + "' does not exist!"
        )
      )
      .andExpect(jsonPath("$.message").value(not(containsString("gate file"))));
  }
}
