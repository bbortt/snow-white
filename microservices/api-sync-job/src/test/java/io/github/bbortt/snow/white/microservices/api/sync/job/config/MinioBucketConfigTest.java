/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static io.github.bbortt.snow.white.microservices.api.sync.job.config.MinioBucketConfig.PUBLIC_BUCKET_POLICY_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.minio.BucketArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class MinioBucketConfigTest {

  private static final String BUCKET_NAME = "snow-white-bucket";

  @Mock
  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  @Mock
  private ApiSyncJobProperties.MinIOProperties minIOPropertiesMock;

  @Mock
  private io.minio.MinioClient minioClientMock;

  private MinioBucketConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();
    doReturn(BUCKET_NAME).when(minIOPropertiesMock).getBucketName();

    fixture = new MinioBucketConfig(apiSyncJobPropertiesMock, minioClientMock);
  }

  @Test
  void isNotEnabled_withoutProperties() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      MinioBucketConfig.class
    );

    contextRunner.run(context ->
      assertThat(context).doesNotHaveBean(MinioBucketConfig.class)
    );
  }

  public static Stream<String> isNotEnabled_withInvalidProperty() {
    return Stream.of("", "false", "foo");
  }

  @MethodSource
  @ParameterizedTest
  void isNotEnabled_withInvalidProperty(String propertyValue) {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      MinioBucketConfig.class
    );

    contextRunner
      .withPropertyValues(PREFIX + ".minio.init-bucket=" + propertyValue)
      .run(context ->
        assertThat(context).doesNotHaveBean(MinioBucketConfig.class)
      );
  }

  @Test
  void isEnabled_whenPropertyIsTrue() {
    doReturn("snow-white-test-bucket")
      .when(minIOPropertiesMock)
      .getBucketName();
    doReturn(minIOPropertiesMock).when(apiSyncJobPropertiesMock).getMinio();

    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      MinioBucketConfig.class
    );

    contextRunner
      .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
      .withBean(io.minio.MinioClient.class, () -> minioClientMock)
      .withPropertyValues(PREFIX + ".minio.init-bucket=true")
      .run(context ->
        assertThat(context).hasSingleBean(MinioBucketConfig.class)
      );
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class Constructor {

    @Test
    void extractsProperties() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class AfterPropertiesSetTest {

    @Test
    void shouldSkipsExistingBucket()
      throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
      ArgumentCaptor<BucketExistsArgs> bucketExistsArgsArgumentCaptor =
        captor();
      doReturn(true)
        .when(minioClientMock)
        .bucketExists(bucketExistsArgsArgumentCaptor.capture());

      fixture.afterPropertiesSet();

      assertThatBucketExistsHasBeenCalled(bucketExistsArgsArgumentCaptor);

      verify(minioClientMock, never()).makeBucket(any(MakeBucketArgs.class));

      assertThatBucketPolicyHasBeenUpdated();
    }

    @Test
    void shouldCreateMissingBucket()
      throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
      ArgumentCaptor<BucketExistsArgs> bucketExistsArgsArgumentCaptor =
        captor();
      doReturn(false)
        .when(minioClientMock)
        .bucketExists(bucketExistsArgsArgumentCaptor.capture());

      fixture.afterPropertiesSet();

      assertThatBucketExistsHasBeenCalled(bucketExistsArgsArgumentCaptor);

      ArgumentCaptor<MakeBucketArgs> makeBucketArgsArgumentCaptor = captor();
      verify(minioClientMock).makeBucket(
        makeBucketArgsArgumentCaptor.capture()
      );
      assertThat(makeBucketArgsArgumentCaptor.getValue())
        .extracting(MakeBucketArgs::bucket)
        .isEqualTo(BUCKET_NAME);

      assertThatBucketPolicyHasBeenUpdated();
    }

    private static void assertThatBucketExistsHasBeenCalled(
      ArgumentCaptor<BucketExistsArgs> bucketExistsArgsArgumentCaptor
    ) {
      assertThat(bucketExistsArgsArgumentCaptor.getValue())
        .extracting(BucketArgs::bucket)
        .isEqualTo(BUCKET_NAME);
    }

    private void assertThatBucketPolicyHasBeenUpdated()
      throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
      ArgumentCaptor<SetBucketPolicyArgs> setBucketPolicyArgsArgumentCaptor =
        captor();
      verify(minioClientMock).setBucketPolicy(
        setBucketPolicyArgsArgumentCaptor.capture()
      );
      assertThat(setBucketPolicyArgsArgumentCaptor.getValue()).satisfies(
        policy -> assertThat(policy.bucket()).isEqualTo(BUCKET_NAME),
        policy ->
          assertThat(policy.config()).isEqualTo(
            PUBLIC_BUCKET_POLICY_TEMPLATE.formatted(
              MinioBucketConfigTest.BUCKET_NAME
            )
          )
      );
    }
  }
}
