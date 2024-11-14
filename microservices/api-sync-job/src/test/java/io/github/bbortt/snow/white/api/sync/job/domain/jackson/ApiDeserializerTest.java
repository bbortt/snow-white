package io.github.bbortt.snow.white.api.sync.job.domain.jackson;

import static io.github.bbortt.snow.white.api.sync.job.domain.ApiLoadStatus.UNLOADED;
import static io.github.bbortt.snow.white.api.sync.job.domain.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.api.sync.job.util.TestUtils.getResourceContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.bbortt.snow.white.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.api.sync.job.domain.Api;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApiDeserializerTest {

  private ApiDeserializer fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiDeserializer(new ApiSyncJobProperties());
  }

  @Test
  void testSuccessfulApiDeserialization() throws IOException {
    var objectMapper = new ObjectMapper()
      .registerModule(new SimpleModule().addDeserializer(Api.class, fixture));

    var content = getResourceContent("ApiDeserializerTest/single-api.json");
    var api = objectMapper.readValue(content, Api.class);

    assertThat(api).satisfies(
      a -> assertThat(a.getTitle()).isEqualTo("Swagger Petstore - OpenAPI 3.1"),
      a -> assertThat(a.getVersion()).isEqualTo("1.0.11"),
      a ->
        assertThat(a.getSourceUrl()).isEqualTo(
          "http://repo.example.com/sir/petstore.yml"
        ),
      a -> assertThat(a.getName()).isEqualTo("swagger-petstore"),
      a -> assertThat(a.getOtelServiceName()).isEqualTo("example-application"),
      a -> assertThat(a.getApiType()).isEqualTo(OPENAPI),
      a -> assertThat(a.getLoadStatus()).isEqualTo(UNLOADED)
    );
  }

  @Test
  void testSuccessfulApiArrayDeserialization() throws IOException {
    var objectMapper = new ObjectMapper()
      .registerModule(new SimpleModule().addDeserializer(Api.class, fixture));

    var content = getResourceContent("ApiDeserializerTest/multiple-apis.json");
    var apis = objectMapper.readValue(content, Api[].class);

    assertThat(apis).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "api-name", "otel-service-name", "version" })
  void testMissingPropertyApiSerialization(String missingProperty)
    throws IOException {
    var objectMapper = new ObjectMapper()
      .registerModule(new SimpleModule().addDeserializer(Api.class, fixture));

    var content = getResourceContent(
      "ApiDeserializerTest/missing-" + missingProperty + ".json"
    );

    assertThatThrownBy(() -> objectMapper.readValue(content, Api.class))
      .isInstanceOf(JsonParseException.class)
      .hasMessageContaining(
        "Mandatory property '",
        missingProperty,
        "' is not provided by API!"
      );
  }
}
