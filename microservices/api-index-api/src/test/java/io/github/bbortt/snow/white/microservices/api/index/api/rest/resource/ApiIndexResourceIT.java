/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.api.index.api.rest.ApiIndexApi.*;
import static io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.bbortt.snow.white.microservices.api.index.AbstractApiIndexApiIT;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class ApiIndexResourceIT extends AbstractApiIndexApiIT {

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private ApiReferenceRepository apiReferenceRepository;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void beforeEachSetup() {
    apiReferenceRepository.deleteAll();
  }

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
        post(PATH_GET_ALL_APIS)
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
  void postRequest_forPrerelease_shouldOverrideExistingEntry()
    throws Exception {
    var prereleaseReference = GetAllApis200ResponseInner.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .content("spec: original")
      .build();

    mockMvc
      .perform(
        post(PATH_GET_ALL_APIS)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(prereleaseReference))
      )
      .andExpect(status().isCreated());

    var updatedContent = GetAllApis200ResponseInner.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .content("spec: updated")
      .build();

    mockMvc
      .perform(
        post(PATH_GET_ALL_APIS)
          .contentType(APPLICATION_JSON)
          .content(jsonMapper.writeValueAsString(updatedContent))
      )
      .andExpect(status().isCreated());

    assertThat(
      apiReferenceRepository.findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
        "serviceName",
        "apiName",
        "apiVersion"
      )
    )
      .isPresent()
      .hasValueSatisfying(r ->
        assertThat(r.getPrereleaseContent()).isEqualTo("spec: updated")
      );
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
        post(PATH_GET_ALL_APIS)
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
      .perform(get(PATH_GET_ALL_APIS).accept(APPLICATION_JSON))
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
        get(PATH_GET_ALL_APIS)
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
        get(PATH_GET_ALL_APIS)
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

  @Test
  void getRequest_withApiNameFilter_shouldReturnMatchingApis()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("api-shared")
        .apiVersion("1.0")
        .sourceUrl("http://a/shared")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-shared")
        .apiVersion("1.0")
        .sourceUrl("http://b/shared")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-other")
        .apiVersion("1.0")
        .sourceUrl("http://b/other")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(PATH_GET_ALL_APIS)
          .param("apiName", "api-shared")
          .accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "2"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(2);
    assertThat(responseJson.get(0).get("apiName").asString()).isEqualTo(
      "api-shared"
    );
    assertThat(responseJson.get(1).get("apiName").asString()).isEqualTo(
      "api-shared"
    );
  }

  @Test
  void getRequest_withServiceNameFilter_shouldMatchServiceNameContainingFilter()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("prefix-ingesting-service")
        .apiName("api-1")
        .apiVersion("1.0")
        .sourceUrl("http://a/1")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("unrelated-service")
        .apiName("api-2")
        .apiVersion("1.0")
        .sourceUrl("http://b/2")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(PATH_GET_ALL_APIS)
          .param("serviceName", "Prefix")
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
      "prefix-ingesting-service"
    );
  }

  @Test
  void getRequest_withApiNameFilter_shouldMatchApiNameContainingFilter()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("prefix-ingesting-api")
        .apiVersion("1.0")
        .sourceUrl("http://a/1")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("unrelated-api")
        .apiVersion("1.0")
        .sourceUrl("http://b/2")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(PATH_GET_ALL_APIS)
          .param("apiName", "Prefix")
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
      "prefix-ingesting-api"
    );
  }

  @Test
  void getRequest_toListServiceNames_shouldReturnDistinctServiceNames()
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
        .otelServiceName("service-a")
        .apiName("api-2")
        .apiVersion("1.0")
        .sourceUrl("http://a/2")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-1")
        .apiVersion("1.0")
        .sourceUrl("http://b/1")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(get(PATH_GET_ALL_SERVICE_NAMES).accept(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(2);
    assertThat(responseJson.get(0).asString()).isEqualTo("service-a");
    assertThat(responseJson.get(1).asString()).isEqualTo("service-b");
  }

  @Test
  void getRequest_toListApiNames_shouldReturnDistinctApiNames()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("api-common")
        .apiVersion("1.0")
        .sourceUrl("http://a/common")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-common")
        .apiVersion("1.0")
        .sourceUrl("http://b/common")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-unique")
        .apiVersion("1.0")
        .sourceUrl("http://b/unique")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(get(PATH_GET_ALL_API_NAMES).accept(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(2);
    assertThat(responseJson.get(0).asString()).isEqualTo("api-common");
    assertThat(responseJson.get(1).asString()).isEqualTo("api-unique");
  }

  @Test
  void getRequest_toListApiNames_withServiceNameFilter_shouldReturnFilteredApiNames()
    throws Exception {
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("api-x")
        .apiVersion("1.0")
        .sourceUrl("http://a/x")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-a")
        .apiName("api-y")
        .apiVersion("1.0")
        .sourceUrl("http://a/y")
        .apiType(OPENAPI)
        .build()
    );
    apiReferenceRepository.save(
      ApiReference.builder()
        .otelServiceName("service-b")
        .apiName("api-z")
        .apiVersion("1.0")
        .sourceUrl("http://b/z")
        .apiType(OPENAPI)
        .build()
    );

    var contentAsString = mockMvc
      .perform(
        get(PATH_GET_ALL_API_NAMES)
          .param("serviceName", "service-a")
          .accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = jsonMapper.readTree(contentAsString);
    assertThat(responseJson.isArray()).isTrue();
    assertThat(responseJson.size()).isEqualTo(2);
    assertThat(responseJson.get(0).asString()).isEqualTo("api-x");
    assertThat(responseJson.get(1).asString()).isEqualTo("api-y");
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
          PATH_GET_API_DETAILS,
          apiReference.getOtelServiceName(),
          apiReference.getApiName(),
          apiReference.getApiVersion()
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
        get(PATH_GET_API_DETAILS, "foo", "bar", "baz").accept(APPLICATION_JSON)
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
          PATH_CHECK_API_EXISTS,
          apiReference.getOtelServiceName(),
          apiReference.getApiName(),
          apiReference.getApiVersion()
        ).accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk());
  }

  @Test
  void getRequest_toCheckIfApiExists_failure() throws Exception {
    mockMvc
      .perform(
        get(PATH_CHECK_API_EXISTS, "foo", "bar", "baz").accept(APPLICATION_JSON)
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void getRequest_toCheckIfApiExists_excludesPrereleasesByDefault()
    throws Exception {
    var prereleaseReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .prereleaseContent("spec: content")
      .build();

    apiReferenceRepository.save(prereleaseReference);

    mockMvc
      .perform(
        get(
          PATH_CHECK_API_EXISTS,
          prereleaseReference.getOtelServiceName(),
          prereleaseReference.getApiName(),
          prereleaseReference.getApiVersion()
        ).accept(APPLICATION_JSON)
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void getRequest_toCheckIfApiExists_includesPrereleases_whenRequested()
    throws Exception {
    var prereleaseReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .prereleaseContent("spec: content")
      .build();

    apiReferenceRepository.save(prereleaseReference);

    mockMvc
      .perform(
        get(
          PATH_CHECK_API_EXISTS,
          prereleaseReference.getOtelServiceName(),
          prereleaseReference.getApiName(),
          prereleaseReference.getApiVersion()
        )
          .param("includePrereleases", "true")
          .accept(APPLICATION_JSON)
      )
      .andExpect(status().isOk());
  }

  @Test
  void getRequest_forRawApiContent_shouldReturnNotFound_whenApiDoesNotExist()
    throws Exception {
    mockMvc
      .perform(get(PATH_GET_RAW_API_CONTENT, "foo", "bar", "baz"))
      .andExpect(status().isNotFound());
  }

  @Test
  void getRequest_forRawApiContent_shouldReturnYaml_whenContentIsOpenApiSpec()
    throws Exception {
    var openApiContent =
      "openapi: 3.1.2\ninfo:\n  title: Test API\n  version: v1";

    var prereleaseReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .prereleaseContent(openApiContent)
      .build();

    apiReferenceRepository.save(prereleaseReference);

    var responseContent = mockMvc
      .perform(
        get(
          PATH_GET_RAW_API_CONTENT,
          prereleaseReference.getOtelServiceName(),
          prereleaseReference.getApiName(),
          prereleaseReference.getApiVersion()
        )
      )
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_YAML_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertThat(responseContent).isEqualTo(openApiContent);
  }

  @Test
  void getRequest_forRawApiContent_shouldReturnPlainText_whenContentIsNotOpenApiSpec()
    throws Exception {
    var rawContent = "some raw spec content that is not openapi";

    var prereleaseReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(OPENAPI)
      .prerelease(true)
      .prereleaseContent(rawContent)
      .build();

    apiReferenceRepository.save(prereleaseReference);

    var responseContent = mockMvc
      .perform(
        get(
          PATH_GET_RAW_API_CONTENT,
          prereleaseReference.getOtelServiceName(),
          prereleaseReference.getApiName(),
          prereleaseReference.getApiVersion()
        )
      )
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, TEXT_PLAIN_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertThat(responseContent).isEqualTo(rawContent);
  }
}
