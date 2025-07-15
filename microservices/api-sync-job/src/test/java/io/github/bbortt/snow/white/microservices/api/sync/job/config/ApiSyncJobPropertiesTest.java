/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class ApiSyncJobPropertiesTest {

  private ApiSyncJobProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiSyncJobProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    public static Stream<String> emptyAndNullString() {
      return Stream.of("", null);
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldPass_whenServiceInterface_baseUrlIsEmptyOrNull(String baseUrl) {
      fixture.getServiceInterface().setBaseUrl(baseUrl);

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @Test
    void shouldPass_whenServiceInterface_propertiesAreSet() {
      fixture.getServiceInterface().setBaseUrl("baseUrl");
      fixture.getServiceInterface().setIndexUri("indexUri");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowException_whenServiceInterface_baseUrlIsSet_andIndexUriIsEmptyOrNull(
      String indexUri
    ) {
      fixture.getServiceInterface().setBaseUrl("baseUrl");
      fixture.getServiceInterface().setIndexUri(indexUri);

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Both 'snow.white.api.sync.job.service-interface.base-url' and 'snow.white.api.sync.job.service-interface.index-uri' must be set!"
        );
    }

    @Test
    void shouldPass_whenBackstage_isUsingCustomAnnotations() {
      fixture.getBackstage().setBaseUrl("baseUrl");
      fixture
        .getBackstage()
        .setCustomVersionAnnotation("customVersionAnnotation");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowException_whenBackstage_baseUrlIsSet_andMinioEndpointIsEmptyOrNull(
      String endpoint
    ) {
      fixture.getBackstage().setBaseUrl("baseUrl");
      fixture.getMinio().setEndpoint(endpoint);

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Backstage API entities can only be parsed if a MinIO storage is configured!"
        );
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowException_whenBackstage_baseUrlIsSet_andMinioEndpointIsSet_butBucketNameIsEmptyOrNull(
      String bucketName
    ) {
      fixture.getBackstage().setBaseUrl("baseUrl");
      fixture.getMinio().setEndpoint("endpoint");
      fixture.getMinio().setBucketName(bucketName);

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Please configure a MinIO bucket name!");
    }
  }
}
