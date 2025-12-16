/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.bbortt.snow.white.microservices.api.index.AbstractApiIndexApiIT;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
public class ApiIndexResourceIT extends AbstractApiIndexApiIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/apis";

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private ApiReferenceRepository apiReferenceRepository;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void postRequest_withAllParameters_shouldBePersisted() throws Exception {
    var newApiReference = GetAllApis200ResponseInner.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(newApiReference))
      )
      .andExpect(status().isCreated());

    assertThat(
      apiReferenceRepository.existsById(
        ApiReference.ApiReferenceId.builder()
          .otelServiceName(newApiReference.getServiceName())
          .apiName(newApiReference.getApiName())
          .apiVersion(newApiReference.getApiVersion())
          .build()
      )
    ).isTrue();
  }

  @Test
  void postRequest_withMissingRequiredParameters_shouldBeReported()
    throws Exception {
    var newApiReference = GetAllApis200ResponseInner.builder()
      .serviceName(null)
      .apiName(null)
      .apiVersion(null)
      .sourceUrl(null)
      .build();

    mockMvc
      .perform(
        post(ENTITY_API_URL)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(newApiReference))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void getRequest_shouldListAllPersistedApis() throws Exception {
    var apiReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .build();

    apiReferenceRepository.save(apiReference);

    var contentAsString = mockMvc
      .perform(get(ENTITY_API_URL).accept(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();

    assertThat(responseJson.get(0))
      .asInstanceOf(type(JsonNode.class))
      .satisfies(
        r ->
          assertThat(r.get("serviceName").asString()).isEqualTo(
            apiReference.getOtelServiceName()
          ),
        r ->
          assertThat(r.get("apiName").asString()).isEqualTo(
            apiReference.getApiName()
          ),
        r ->
          assertThat(r.get("apiVersion").asString()).isEqualTo(
            apiReference.getApiVersion()
          ),
        r ->
          assertThat(r.get("sourceUrl").asString()).isEqualTo(
            apiReference.getSourceUrl()
          ),
        r -> assertThat(r.get("apiType").asString()).isEqualTo(OPENAPI.name())
      );
  }

  @Test
  void getRequest_forEntityDetails_shouldListSingleEntity() throws Exception {
    var apiReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .build();

    apiReferenceRepository.save(apiReference);

    var contentAsString = mockMvc
      .perform(
        get(
          format(
            "%s/%s/%s/%s",
            ENTITY_API_URL,
            apiReference.getOtelServiceName(),
            apiReference.getApiName(),
            apiReference.getApiVersion()
          )
        ).accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);

    assertThat(responseJson)
      .asInstanceOf(type(JsonNode.class))
      .satisfies(
        r ->
          assertThat(r.get("serviceName").asString()).isEqualTo(
            apiReference.getOtelServiceName()
          ),
        r ->
          assertThat(r.get("apiName").asString()).isEqualTo(
            apiReference.getApiName()
          ),
        r ->
          assertThat(r.get("apiVersion").asString()).isEqualTo(
            apiReference.getApiVersion()
          ),
        r ->
          assertThat(r.get("sourceUrl").asString()).isEqualTo(
            apiReference.getSourceUrl()
          ),
        r -> assertThat(r.get("apiType").asString()).isEqualTo(OPENAPI.name())
      );
  }

  @Test
  void getRequest_forEntityDetails_shouldReturnNotFound() throws Exception {
    mockMvc
      .perform(
        get(format("%s/%s/%s/%s", ENTITY_API_URL, "foo", "bar", "baz")).accept(
          APPLICATION_JSON
        )
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void getRequest_toCheckIfApiExists_success() throws Exception {
    var apiReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .build();

    apiReferenceRepository.save(apiReference);

    mockMvc
      .perform(
        get(
          format(
            "%s/%s/%s/%s/exists",
            ENTITY_API_URL,
            apiReference.getOtelServiceName(),
            apiReference.getApiName(),
            apiReference.getApiVersion()
          )
        ).accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk());
  }

  @Test
  void getRequest_toCheckIfApiExists_failure() throws Exception {
    mockMvc
      .perform(
        get(
          format("%s/%s/%s/%s/exists", ENTITY_API_URL, "foo", "bar", "baz")
        ).accept(APPLICATION_JSON)
      )
      .andExpect(status().isNotFound());
  }
}
