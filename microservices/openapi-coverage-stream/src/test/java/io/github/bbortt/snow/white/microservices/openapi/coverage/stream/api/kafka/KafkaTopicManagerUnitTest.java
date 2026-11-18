/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka;

import static io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent.MAX_SERIALIZED_BYTES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class KafkaTopicManagerUnitTest {

  public static final String TEST_RESPONSE_TOPIC =
    "KafkaTopicManagerTest:response";
  public static final String TEST_REQUEST_TOPIC =
    "KafkaTopicManagerTest:request";

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    var openApiCoverageServiceProperties =
      new OpenApiCoverageStreamProperties();
    openApiCoverageServiceProperties.setCalculationRequestTopic(
      TEST_REQUEST_TOPIC
    );
    openApiCoverageServiceProperties.setOpenapiCalculationResponseTopic(
      TEST_RESPONSE_TOPIC
    );

    fixture = new KafkaTopicManager(openApiCoverageServiceProperties);
  }

  @Test
  void shouldBeEnabled_whenPropertyIsSet() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      KafkaTopicManager.class
    );

    var openApiCoverageServicePropertiesMock = mock(
      OpenApiCoverageStreamProperties.class
    );

    contextRunner
      .withBean(
        OpenApiCoverageStreamProperties.class,
        () -> openApiCoverageServicePropertiesMock
      )
      .withPropertyValues(PREFIX + ".init-topics=true")
      .run(context ->
        assertThat(context)
          .asInstanceOf(type(AssertableApplicationContext.class))
          .satisfies(
            c -> assertThat(c).hasSingleBean(KafkaTopicManager.class),
            c ->
              assertThat(c)
                .getBean("calculationRequestTopic", NewTopic.class)
                .isNotNull(),
            c ->
              assertThat(c)
                .getBean("openapiCalculationResponseTopic", NewTopic.class)
                .isNotNull()
          )
      );

    verify(openApiCoverageServicePropertiesMock).getCalculationRequestTopic();
    verify(
      openApiCoverageServicePropertiesMock
    ).getOpenapiCalculationResponseTopic();
  }

  @Test
  void shouldNotBeEnabled_ifPropertyIsNotSet() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      KafkaTopicManager.class
    );

    contextRunner.run(context ->
      assertThat(context).doesNotHaveBean(KafkaTopicManager.class)
    );
  }

  @Test
  void testInboundTopicCreation() {
    var inboundTopic = fixture.calculationRequestTopic();

    assertThat(inboundTopic.name()).isEqualTo(TEST_REQUEST_TOPIC);
  }

  @Test
  void testOutboundTopicCreation() {
    var outboundTopic = fixture.openapiCalculationResponseTopic();

    assertThat(outboundTopic.name()).isEqualTo(TEST_RESPONSE_TOPIC);
  }

  /**
   * A response topic holding less than this producer may send would reject the record the producer
   * was configured to allow - the failure the limit exists to prevent.
   */
  @Test
  void outboundTopicAdmitsTheLargestCoverageResponse() {
    var outboundTopic = fixture.openapiCalculationResponseTopic();

    assertThat(outboundTopic.configs()).containsEntry(
      TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
      String.valueOf(MAX_SERIALIZED_BYTES)
    );
  }

  /**
   * The producer limit stays in {@code application.yaml}, where the chart's environment overrides
   * still reach it - which puts the number in a second place. This fails the build when the two
   * drift apart.
   */
  @Test
  void producerRequestSizeMatchesTheEventLimit() {
    assertThat(applicationYaml())
      .extracting(propertySource ->
        propertySource.getProperty(
          "spring.kafka.streams.properties.producer.max.request.size"
        )
      )
      .hasToString(String.valueOf(MAX_SERIALIZED_BYTES));
  }

  /**
   * Declaring the limit only reaches a topic this service creates. On a cluster that already holds
   * the response topic the declaration is ignored, the topic keeps Kafka's 1 MiB default, and
   * raising the producer alone just moves the rejection to the broker.
   */
  @Test
  void existingTopicConfigurationIsModified() {
    assertThat(applicationYaml())
      .extracting(propertySource ->
        propertySource.getProperty("spring.kafka.admin.modify-topic-configs")
      )
      .hasToString("true");
  }

  private static PropertySource<?> applicationYaml() {
    var propertySources = assertDoesNotThrow(() ->
      new YamlPropertySourceLoader().load(
        "application.yaml",
        new ClassPathResource("config/application.yaml")
      )
    );

    assertThat(propertySources).hasSize(1);

    return propertySources.getFirst();
  }
}
