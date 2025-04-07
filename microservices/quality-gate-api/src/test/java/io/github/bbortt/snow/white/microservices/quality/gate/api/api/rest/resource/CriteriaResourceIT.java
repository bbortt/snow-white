package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.IntegrationTest;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class CriteriaResourceIT {

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
      .andExpect(jsonPath("$.length()").value(7))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var openApiCriteria = objectMapper.readValue(
      content,
      new TypeReference<List<OpenApiCriterion>>() {}
    );

    assertThat(openApiCriteria).containsExactlyInAnyOrderElementsOf(
      stream(OpenApiCriteria.values())
        .map(c ->
          OpenApiCriterion.builder()
            .id(c.name())
            .name(c.getLabel())
            .description(c.getDescription())
            .build()
        )
        .toList()
    );
  }
}
