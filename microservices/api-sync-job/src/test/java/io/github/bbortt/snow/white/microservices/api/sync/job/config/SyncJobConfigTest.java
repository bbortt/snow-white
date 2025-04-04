package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson.ApiInformationDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class SyncJobConfigTest {

  private SyncJobConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SyncJobConfig();
  }

  @Test
  void jsonCustomizerRegistersApiDeserializerForObjectMapper() {
    Jackson2ObjectMapperBuilderCustomizer jsonCustomizer =
      fixture.jsonCustomizer(new ApiSyncJobProperties());

    var jackson2ObjectMapperBuilderMock = mock(
      Jackson2ObjectMapperBuilder.class
    );
    jsonCustomizer.customize(jackson2ObjectMapperBuilderMock);

    verify(jackson2ObjectMapperBuilderMock).deserializers(
      any(ApiInformationDeserializer.class)
    );
  }
}
