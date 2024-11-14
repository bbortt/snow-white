package io.github.bbortt.snow.white.api.sync.job;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.bbortt.snow.white.api.sync.job.util.TestUtils.getResourceContent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import com.redis.testcontainers.RedisContainer;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@IntegrationTest
@Testcontainers
class SyncJobIT {

  @Container
  private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
    DockerImageName.parse("redis:7.4.1-alpine")
  ).withExposedPorts(6379);

  @Value("${wiremock.server.baseUrl}")
  private String wiremockServerBaseUrl;

  @Autowired
  private SyncJob fixture;

  @DynamicPropertySource
  private static void registerRedisProperties(
    DynamicPropertyRegistry registry
  ) {
    registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.redis.port", () ->
      REDIS_CONTAINER.getMappedPort(6379).toString()
    );
  }

  public static Stream<String> indexContentTypes() {
    return Stream.of(APPLICATION_JSON_VALUE, APPLICATION_OCTET_STREAM_VALUE);
  }

  @MethodSource("indexContentTypes")
  @ParameterizedTest
  void queryAndSafeApiCatalogArtifacts(String contentType) throws IOException {
    var exampleResponse = getResourceContent(
      "sir/full-example-response.json"
    ).replace("${wiremock.server.baseUrl}", wiremockServerBaseUrl);

    stubFor(
      get("/sir/index").willReturn(
        okForContentType(contentType, exampleResponse)
      )
    );

    assertDoesNotThrow(() -> fixture.queryAndSafeApiCatalogArtifacts());
  }
}
