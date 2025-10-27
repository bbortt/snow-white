/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.util.TestUtils.getResourceContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class ApiInformationDeserializerTest {

  private ApiInformationDeserializer fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiInformationDeserializer(new ApiSyncJobProperties());
  }

  private JsonMapper getJsonMapper() {
    return new JsonMapper.Builder(JsonFactory.builder().build())
      .addModule(
        new SimpleModule().addDeserializer(ApiInformation.class, fixture)
      )
      .build();
  }

  @Test
  void testSuccessfulApiDeserialization() throws IOException {
    var content = getResourceContent("ApiDeserializerTest/single-api.json");
    var api = getJsonMapper().readValue(content, ApiInformation.class);

    var title = "Swagger Petstore - OpenAPI 3.1";
    assertThat(api).satisfies(
      a -> assertThat(a.getTitle()).isEqualTo(title),
      a -> assertThat(a.getVersion()).isEqualTo("1.2.3"),
      a ->
        assertThat(a.getSourceUrl()).isEqualTo(
          "http://repo.example.com/sir/petstore.yml"
        ),
      a -> assertThat(a.getName()).isEqualTo(title),
      a -> assertThat(a.getServiceName()).isEqualTo("example-application"),
      a -> assertThat(a.getApiType()).isEqualTo(OPENAPI),
      a -> assertThat(a.getLoadStatus()).isEqualTo(UNLOADED)
    );
  }

  @Test
  void testSuccessfulApiDeserializationWithExtraName() throws IOException {
    var apiSyncJobProperties = new ApiSyncJobProperties();
    apiSyncJobProperties
      .getServiceInterface()
      .setApiNameProperty("oas.info.x-api-name");

    fixture = new ApiInformationDeserializer(apiSyncJobProperties);

    var content = getResourceContent("ApiDeserializerTest/single-api.json");
    var api = getJsonMapper().readValue(content, ApiInformation.class);

    assertThat(api).satisfies(
      a -> assertThat(a).hasNoNullFieldsOrProperties(),
      a -> assertThat(a.getTitle()).isEqualTo("Swagger Petstore - OpenAPI 3.1"),
      a -> assertThat(a.getName()).isEqualTo("swagger-petstore"),
      a -> assertThat(a.getServiceName()).isEqualTo("example-application")
    );
  }

  @Test
  void testSuccessfulApiDeserializationWithExtraVersion() throws IOException {
    var apiSyncJobProperties = new ApiSyncJobProperties();
    apiSyncJobProperties
      .getServiceInterface()
      .setApiVersionProperty("oas.info.x-api-version");

    fixture = new ApiInformationDeserializer(apiSyncJobProperties);

    var content = getResourceContent("ApiDeserializerTest/single-api.json");
    var api = getJsonMapper().readValue(content, ApiInformation.class);

    assertThat(api).satisfies(
      a -> assertThat(a).hasNoNullFieldsOrProperties(),
      a -> assertThat(a.getVersion()).isEqualTo("2.3.4")
    );
  }

  @Test
  void testSuccessfulApiArrayDeserializationOfMultipleApis()
    throws IOException {
    var content = getResourceContent("ApiDeserializerTest/multiple-apis.json");
    var apis = getJsonMapper().readValue(content, ApiInformation[].class);

    assertThat(apis).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "title", "otel-service-name", "version" })
  void testMissingPropertyApiSerialization(String missingProperty)
    throws IOException {
    var content = getResourceContent(
      "ApiDeserializerTest/missing-" + missingProperty + ".json"
    );

    assertThatThrownBy(() ->
      getJsonMapper().readValue(content, ApiInformation.class)
    )
      .isInstanceOf(ApiCatalogException.class)
      .hasMessageContaining(
        "Mandatory property '",
        missingProperty,
        "' is not provided by API!"
      );
  }
}
