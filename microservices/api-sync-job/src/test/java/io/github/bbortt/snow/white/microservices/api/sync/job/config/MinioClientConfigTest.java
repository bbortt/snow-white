/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class MinioClientConfigTest {

  @Mock
  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  @Mock
  private ApiSyncJobProperties.MinIOProperties minIOPropertiesMock;

  private MinioClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new MinioClientConfig();
  }

  @Test
  void isNotEnabledWithoutProperties() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(MinioClientConfig.class);

    contextRunner.run(context ->
      assertThat(context).doesNotHaveBean(MinioClientConfig.class)
    );
  }

  @Test
  void isEnabledWhenMinioBaseUrlIsPresent() {
    var baseUrl = "baseUrl";
    doReturn(baseUrl).when(minIOPropertiesMock).getEndpoint();

    doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();

    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(MinioClientConfig.class);

    contextRunner
      .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
      .withPropertyValues(PREFIX + ".minio.endpoint=" + baseUrl)
      .run(context ->
        assertThat(context)
          .asInstanceOf(type(AssertableApplicationContext.class))
          .satisfies(
            c -> assertThat(c).hasSingleBean(MinioClientConfig.class),
            c -> assertThat(c).hasSingleBean(io.minio.MinioClient.class)
          )
      );

    verify(minIOPropertiesMock).getEndpoint();
    verify(minIOPropertiesMock).getAccessKey();
    verifyNoMoreInteractions(minIOPropertiesMock);
  }

  @Nested
  class MinioClient {

    private static final String BASE_URL = "baseUrl";

    @BeforeEach
    void beforeEachSetup() {
      doReturn(BASE_URL).when(minIOPropertiesMock).getEndpoint();
      doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();
    }

    public static Stream<String> constructsClient_withoutCredentials() {
      return Stream.of(null, "", " ");
    }

    @MethodSource
    @ParameterizedTest
    void constructsClient_withoutCredentials(String accessKey) {
      doReturn(accessKey).when(minIOPropertiesMock).getAccessKey();

      io.minio.MinioClient minioClient = fixture.minioClient(
        apiSyncJobPropertiesMock
      );

      assertThat(minioClient).isNotNull();

      verify(minIOPropertiesMock).getEndpoint();
      verify(minIOPropertiesMock).getAccessKey();
      verifyNoMoreInteractions(minIOPropertiesMock);
    }

    @Test
    void constructsClient_withCredentials() {
      var accessKey = "accessKey";
      doReturn(accessKey).when(minIOPropertiesMock).getAccessKey();

      var secretKey = "secretKey";
      doReturn(secretKey).when(minIOPropertiesMock).getSecretKey();

      io.minio.MinioClient minioClient = fixture.minioClient(
        apiSyncJobPropertiesMock
      );

      assertThat(minioClient).isNotNull();

      verify(minIOPropertiesMock).getEndpoint();
      verify(minIOPropertiesMock, times(2)).getAccessKey();
      verify(minIOPropertiesMock, times(2)).getSecretKey();
    }
  }
}
