/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractQualityGateApiIT;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CriteriaResourceIT extends AbstractQualityGateApiIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/criteria";
  private static final String OPENAPI_ENTITY_API_URL =
    ENTITY_API_URL + "/openapi";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void findAllOpenapiCriteria() throws Exception {
    var content = mockMvc
      .perform(get(OPENAPI_ENTITY_API_URL))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.length()").value(OpenApiCriteria.values().length))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var openApiCriteria = objectMapper.readValue(
      content,
      new TypeReference<List<OpenApiCriterion>>() {}
    );

    assertThat(openApiCriteria).containsExactly(
        stream(OpenApiCriteria.values())
          .map(c ->
            OpenApiCriterion.builder()
              .id(c.name())
              .name(c.getLabel())
              .description(c.getDescription())
              .build()
          )
          .sorted(comparing(OpenApiCriterion::getName))
          .toArray(OpenApiCriterion[]::new)
      );
  }
}
