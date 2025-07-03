/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson.ApiInformationDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({ SpringExtension.class })
class SyncJobConfigTest {

  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  private SyncJobConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SyncJobConfig();
  }

  @Nested
  class JsonCustomizer {

    @Test
    void isBean() {
      var runner = new ApplicationContextRunner().withUserConfiguration(
        SyncJobConfig.class
      );

      runner
        .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
        .run(context ->
          assertThat(context)
            .hasSingleBean(Jackson2ObjectMapperBuilderCustomizer.class)
            .getBean(
              "jsonCustomizer",
              Jackson2ObjectMapperBuilderCustomizer.class
            )
            .isNotNull()
        );
    }

    @Test
    void registersApiDeserializerForObjectMapper() {
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
}
