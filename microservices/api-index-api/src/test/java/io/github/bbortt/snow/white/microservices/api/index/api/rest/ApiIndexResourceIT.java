/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
class ApiIndexResourceIT extends AbstractApiIndexApiIT {

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

  @Test
  void getRequest_withServiceNameFilter_shouldReturnMatchingApis()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("api-1")
        .apiVersion("1.0")
        .sourceUrl("http://a/1")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-2")
        .apiVersion("1.0")
        .sourceUrl("http://b/2")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(ENTITY_API_URL)
          .param("serviceName", "service-a")
          .accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(1);
    assertThat(responseJson.get(0).get("serviceName").asString()).isEqualTo(
      "service-a"
    );
  }

  @Test
  void getRequest_withServiceNameAndApiNameFilter_shouldReturnMatchingApis()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-c")
        .apiName("api-x")
        .apiVersion("1.0")
        .sourceUrl("http://c/x")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-c")
        .apiName("api-y")
        .apiVersion("1.0")
        .sourceUrl("http://c/y")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(ENTITY_API_URL)
          .param("serviceName", "service-c")
          .param("apiName", "api-x")
          .accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(1);
    assertThat(responseJson.get(0).get("apiName").asString()).isEqualTo(
      "api-x"
    );
  }
}
