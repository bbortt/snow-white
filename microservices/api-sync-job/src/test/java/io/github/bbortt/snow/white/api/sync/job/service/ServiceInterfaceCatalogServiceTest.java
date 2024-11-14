package io.github.bbortt.snow.white.api.sync.job.service;

import static io.github.bbortt.snow.white.api.sync.job.domain.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.api.sync.job.domain.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.api.sync.job.parser.ParsingMode.STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.api.sync.job.domain.Api;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class ServiceInterfaceCatalogServiceTest {

  private static final String API_TITLE = "Mostly Harmless";
  private static final String BASE_URL = "http://localhost:8080";

  @Mock
  private RestClient restCLientMock;

  @Mock
  private RestClient.Builder restCLientBuilderMock;

  private ApiSyncJobProperties apiSyncJobProperties;

  @BeforeEach
  void beforeEachSetup() {
    apiSyncJobProperties = new ApiSyncJobProperties();
    apiSyncJobProperties.getServiceInterface().setBaseUrl(BASE_URL);
    apiSyncJobProperties.getServiceInterface().setIndexUri("/sir/index");

    doReturn(restCLientBuilderMock)
      .when(restCLientBuilderMock)
      .baseUrl(BASE_URL);
    doReturn(restCLientMock).when(restCLientBuilderMock).build();
  }

  @Nested
  class Constructor {

    @Test
    void createsRestClient() {
      assertThat(
        new ServiceInterfaceCatalogService(
          restCLientBuilderMock,
          apiSyncJobProperties
        )
      ).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class ValidateApiInformationFromIndex {

    @Test
    void ignoresApiWithoutSourceUrlInGracefulMode() {
      apiSyncJobProperties.getServiceInterface().setParsingMode(GRACEFUL);
      var fixture = new ServiceInterfaceCatalogService(
        restCLientBuilderMock,
        apiSyncJobProperties
      );

      var api = new Api().withTitle(API_TITLE).withSourceUrl(null);

      var resultingApi = fixture.validateApiInformationFromIndex(api);

      assertThat(resultingApi.getLoadStatus()).isEqualTo(NO_SOURCE);
    }

    @Test
    void throwsExceptionWithoutSourceUrlInStrictMode() {
      apiSyncJobProperties.getServiceInterface().setParsingMode(STRICT);
      var fixture = new ServiceInterfaceCatalogService(
        restCLientBuilderMock,
        apiSyncJobProperties
      );

      var api = new Api().withTitle(API_TITLE).withSourceUrl(null);

      assertThatThrownBy(() -> fixture.validateApiInformationFromIndex(api))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index without source URL: Mostly Harmless!"
        );
    }

    @Test
    void ignoresApiWithoutApiTypeInGracefulMode() {
      apiSyncJobProperties.getServiceInterface().setParsingMode(GRACEFUL);
      var fixture = new ServiceInterfaceCatalogService(
        restCLientBuilderMock,
        apiSyncJobProperties
      );

      var api = new Api()
        .withTitle(API_TITLE)
        .withSourceUrl("http://localhost:8080/petstore.yml")
        .withApiType(null);

      var resultingApi = fixture.validateApiInformationFromIndex(api);

      assertThat(resultingApi.getLoadStatus()).isEqualTo(LOAD_FAILED);
    }

    @Test
    void throwsExceptionWithoutApiTypeInStrictMode() {
      apiSyncJobProperties.getServiceInterface().setParsingMode(STRICT);
      var fixture = new ServiceInterfaceCatalogService(
        restCLientBuilderMock,
        apiSyncJobProperties
      );

      var api = new Api()
        .withTitle(API_TITLE)
        .withSourceUrl("http://localhost:8080/petstore.yml")
        .withApiType(null);

      assertThatThrownBy(() -> fixture.validateApiInformationFromIndex(api))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index without type definition: Mostly Harmless!"
        );
    }
  }
}
