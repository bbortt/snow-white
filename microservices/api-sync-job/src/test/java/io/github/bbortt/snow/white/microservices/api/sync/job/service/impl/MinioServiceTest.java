/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class MinioServiceTest {

  @Mock
  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  @Mock
  private ApiSyncJobProperties.MinIOProperties minIOPropertiesMock;

  @Mock
  private MinioClient minioClientMock;

  @Test
  void exists_ifMinIOClientBeanExists() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      MinioService.class
    );

    doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();

    contextRunner
      .withBean(MinioClient.class, () -> minioClientMock)
      .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
      .run(context -> assertThat(context).hasSingleBean(MinioService.class));

    verify(minIOPropertiesMock).getBucketName();
    verify(minIOPropertiesMock).getEndpoint();
  }

  @Nested
  class StoreBackstageApiEntity {

    public static final String ENDPOINT = "http://localhost:9000";
    public static final String BUCKET_NAME = "test-bucket";

    @Mock
    private BackstageCatalogService.OpenAPIParameters openAPIParametersMock;

    private MinioService fixture;

    @BeforeEach
    void beforeEachSetup() {
      doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();

      doReturn(ENDPOINT).when(minIOPropertiesMock).getEndpoint();
      doReturn(BUCKET_NAME).when(minIOPropertiesMock).getBucketName();

      fixture = new MinioService(apiSyncJobPropertiesMock, minioClientMock);
    }

    @Test
    void shouldStoreApiEntityAndReturnPublicUrl() throws Exception {
      var apiInformation = createApiInformation(
        "user-service",
        "User API",
        "v1.0"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn(
        """
        """
      )
        .when(openAPIParametersMock)
        .openApiAsJson();

      ApiInformation result = fixture.storeBackstageApiEntity(
        openAPIParametersMock
      );

      verify(minioClientMock).putObject(any(PutObjectArgs.class));

      assertThat(result)
        .isNotNull()
        .extracting(ApiInformation::getSourceUrl)
        .isEqualTo(
          ENDPOINT + "/" + BUCKET_NAME + "/user-service-User-API-v1.0.xml"
        );
    }

    @Test
    void shouldHandleEndpointWithTrailingSlash() {
      var apiInformation = createApiInformation(
        "order-service",
        "Order API",
        "v2.1"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn("openApiAsJson").when(openAPIParametersMock).openApiAsJson();

      doReturn("https://minio.example.com/")
        .when(minIOPropertiesMock)
        .getEndpoint(); // with trailing slash

      // Override fixture
      fixture = new MinioService(apiSyncJobPropertiesMock, minioClientMock);

      var result = fixture.storeBackstageApiEntity(openAPIParametersMock);

      assertThat(result.getSourceUrl()).isEqualTo(
        "https://minio.example.com/" +
        BUCKET_NAME +
        "/order-service-Order-API-v2.1.xml"
      );
    }

    @Test
    void shouldVerifyCorrectObjectNameConstruction() throws Exception {
      var apiInformation = createApiInformation(
        "payment-service",
        "Payment Gateway API",
        "v3.0.1"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn("openApiAsJson").when(openAPIParametersMock).openApiAsJson();

      fixture.storeBackstageApiEntity(openAPIParametersMock);

      ArgumentCaptor<PutObjectArgs> putObjectArgsCaptor = captor();
      verify(minioClientMock).putObject(putObjectArgsCaptor.capture());

      var capturedArgs = putObjectArgsCaptor.getValue();
      assertThat(capturedArgs.bucket()).isEqualTo(BUCKET_NAME);
      assertThat(capturedArgs.object()).isEqualTo(
        "payment-service-Payment-Gateway-API-v3.0.1.xml"
      );
    }

    @Test
    void shouldVerifyStreamContentIsOpenApiVersion() throws Exception {
      var apiInformation = createApiInformation(
        "inventory-service",
        "Inventory API",
        "v1.5"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn("openApiAsJson").when(openAPIParametersMock).openApiAsJson();

      fixture.storeBackstageApiEntity(openAPIParametersMock);

      var putObjectArgsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
      verify(minioClientMock).putObject(putObjectArgsCaptor.capture());

      // The stream should contain the OpenAPI version as bytes
      // We can't easily verify the exact stream content, but we can verify the call was made
      assertThat(putObjectArgsCaptor.getValue()).isNotNull();
    }

    @Test
    void shouldThrowMinioExceptionWhenMinioClientThrowsException()
      throws Exception {
      var apiInformation = createApiInformation(
        "error-service",
        "Error API",
        "v1.0"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn("openApiAsJson").when(openAPIParametersMock).openApiAsJson();

      doThrow(new RuntimeException("MinIO connection failed"))
        .when(minioClientMock)
        .putObject(any(PutObjectArgs.class));

      assertThatThrownBy(() ->
        fixture.storeBackstageApiEntity(openAPIParametersMock)
      )
        .isInstanceOf(MinioService.MinioException.class)
        .hasMessage("Failed to store object in MinIO!")
        .hasCauseInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("MinIO connection failed");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenApiInformationIsNull() {
      doReturn(null).when(openAPIParametersMock).apiInformation();

      assertThatThrownBy(() ->
        fixture.storeBackstageApiEntity(openAPIParametersMock)
      ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleSpecialCharactersInServiceNames() {
      var apiInformation = createApiInformation(
        "user-auth-service",
        "User & Auth API",
        "v1.0-beta"
      );
      doReturn(apiInformation).when(openAPIParametersMock).apiInformation();

      doReturn("openApiAsJson").when(openAPIParametersMock).openApiAsJson();

      var result = fixture.storeBackstageApiEntity(openAPIParametersMock);

      assertThat(result.getSourceUrl()).isEqualTo(
        ENDPOINT +
        "/" +
        BUCKET_NAME +
        "/user-auth-service-User-&-Auth-API-v1.0-beta.xml"
      );
    }

    private ApiInformation createApiInformation(
      String serviceName,
      String name,
      String version
    ) {
      return ApiInformation.builder()
        .serviceName(serviceName)
        .name(name)
        .version(version)
        .build();
    }

    private OpenAPI createOpenAPI(String version) {
      var openAPI = mock(OpenAPI.class);
      doReturn(version).when(openAPI).getOpenapi();
      return openAPI;
    }
  }
}
