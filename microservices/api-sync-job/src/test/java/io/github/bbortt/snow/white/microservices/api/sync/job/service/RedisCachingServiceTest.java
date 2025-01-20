package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;
import io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis.RedisCachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class RedisCachingServiceTest {

  @Mock
  private ApiEndpointRepository apiEndpointRepositoryMock;

  @Captor
  private ArgumentCaptor<ApiEndpointEntry> apiEndpointEntryCaptor;

  private RedisCachingService service;

  @BeforeEach
  void beforeEachSetup() {
    service = new RedisCachingService(apiEndpointRepositoryMock);
  }

  @Nested
  class PublishApiInformation {

    @Test
    void shouldSaveApiInformationToRepository() {
      var serviceName = "test-service";
      var apiName = "test-api";
      var apiVersion = "1.2.3";
      var sourceUrl = "https://sample.repository";

      var api = new Api()
        .withOtelServiceName(serviceName)
        .withName(apiName)
        .withVersion(apiVersion)
        .withSourceUrl(sourceUrl);

      doAnswer(returnsFirstArg())
        .when(apiEndpointRepositoryMock)
        .save(any(ApiEndpointEntry.class));

      service.publishApiInformation(api);

      verify(apiEndpointRepositoryMock).save(apiEndpointEntryCaptor.capture());

      assertThat(apiEndpointEntryCaptor.getValue()).satisfies(
        e ->
          assertThat(e.getId()).isEqualTo(
            serviceName + ":" + apiName + ":" + apiVersion
          ),
        e -> assertThat(e.getOtelServiceName()).isEqualTo(serviceName),
        e -> assertThat(e.getApiName()).isEqualTo(apiName),
        e -> assertThat(e.getApiVersion()).isEqualTo(apiVersion),
        e -> assertThat(e.getSourceUrl()).isEqualTo(sourceUrl)
      );
    }
  }
}
