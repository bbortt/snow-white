/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry.SnowWhiteApiInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

class KafkaListenerInformationRegistryUnitTest {

  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";

  private KafkaListenerInformationRegistry fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaListenerInformationRegistry();
  }

  @Test
  void unknownTopicIsNotRegistered() {
    assertThat(fixture.findByTopic("nothing-listens-here")).isNull();
  }

  @Test
  void methodAnnotationIsRegisteredByTopic() {
    fixture.postProcessAfterInitialization(
      new MethodAnnotatedListener(),
      "methodAnnotatedListener"
    );

    assertThat(fixture.findByTopic("method-topic")).isEqualTo(
      new SnowWhiteApiInformation(
        API_NAME,
        API_VERSION,
        SERVICE_NAME,
        "receiveOnMethodTopic"
      )
    );
  }

  @Test
  void classAnnotationAppliesToEveryTopicOfTheBean() {
    fixture.postProcessAfterInitialization(
      new ClassAnnotatedListener(),
      "classAnnotatedListener"
    );

    var expected = new SnowWhiteApiInformation(
      API_NAME,
      API_VERSION,
      SERVICE_NAME,
      ""
    );

    assertThat(fixture.findByTopic("first-topic")).isEqualTo(expected);
    assertThat(fixture.findByTopic("second-topic")).isEqualTo(expected);
  }

  @Test
  void methodAnnotationOverridesClassAnnotation() {
    fixture.postProcessAfterInitialization(
      new MergingListener(),
      "mergingListener"
    );

    assertThat(fixture.findByTopic("merging-topic")).isEqualTo(
      new SnowWhiteApiInformation(
        API_NAME,
        API_VERSION,
        SERVICE_NAME,
        "receiveOnMergingTopic"
      )
    );
  }

  @Test
  void listenerWithoutSnowWhiteInformationIsIgnored() {
    fixture.postProcessAfterInitialization(
      new UnannotatedListener(),
      "unannotatedListener"
    );

    assertThat(fixture.findByTopic("unannotated-topic")).isNull();
  }

  @Test
  void beanWithoutKafkaListenerIsIgnored() {
    var bean = new NotAListener();

    assertThat(
      fixture.postProcessAfterInitialization(bean, "notAListener")
    ).isSameAs(bean);
  }

  @Test
  void duplicateTopicKeepsTheLastDeclaration() {
    fixture.postProcessAfterInitialization(
      new ClassAnnotatedListener(),
      "classAnnotatedListener"
    );
    fixture.postProcessAfterInitialization(
      new ConflictingListener(),
      "conflictingListener"
    );

    assertThat(fixture.findByTopic("first-topic")).isEqualTo(
      new SnowWhiteApiInformation(
        "other-api",
        API_VERSION,
        SERVICE_NAME,
        "receiveOnFirstTopic"
      )
    );
  }

  @Test
  void repeatedPostProcessingOfTheSameBeanDoesNotConflict() {
    var bean = new ClassAnnotatedListener();

    fixture.postProcessAfterInitialization(bean, "classAnnotatedListener");
    fixture.postProcessAfterInitialization(bean, "classAnnotatedListener");

    assertThat(fixture.findByTopic("first-topic")).isEqualTo(
      new SnowWhiteApiInformation(API_NAME, API_VERSION, SERVICE_NAME, "")
    );
  }

  @Test
  void missingServiceNameFailsFast() {
    assertThatThrownBy(() ->
      fixture.postProcessAfterInitialization(
        new IncompleteListener(),
        "incompleteListener"
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("IncompleteListener#receiveOnIncompleteTopic")
      .hasMessageContaining("serviceName=''");
  }

  @Test
  void missingApiNameFailsFast() {
    assertThatThrownBy(() ->
      fixture.postProcessAfterInitialization(
        new MissingApiNameListener(),
        "missingApiNameListener"
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("apiName=''");
  }

  @Test
  void missingApiVersionFailsFast() {
    assertThatThrownBy(() ->
      fixture.postProcessAfterInitialization(
        new MissingApiVersionListener(),
        "missingApiVersionListener"
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("apiVersion=''");
  }

  @Nested
  class ResolveEmbeddedValueTest {

    @Test
    void topicPlaceholderIsResolved() {
      fixture.setEmbeddedValueResolver(value ->
        "${placeholder.topic}".equals(value) ? "resolved-topic" : value
      );

      fixture.postProcessAfterInitialization(
        new PlaceholderListener(),
        "placeholderListener"
      );

      assertThat(fixture.findByTopic("resolved-topic")).isNotNull();
      assertThat(fixture.findByTopic("${placeholder.topic}")).isNull();
    }

    @Test
    void topicResolvingToNullIsSkipped() {
      fixture.setEmbeddedValueResolver(value -> null);

      fixture.postProcessAfterInitialization(
        new PlaceholderListener(),
        "placeholderListener"
      );

      assertThat(fixture.findByTopic("${placeholder.topic}")).isNull();
    }
  }

  static class MethodAnnotatedListener {

    @SnowWhiteInformation(
      serviceName = SERVICE_NAME,
      apiName = API_NAME,
      apiVersion = API_VERSION,
      operationId = "receiveOnMethodTopic"
    )
    @KafkaListener(topics = "method-topic")
    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(
    serviceName = SERVICE_NAME,
    apiName = API_NAME,
    apiVersion = API_VERSION
  )
  static class ClassAnnotatedListener {

    @KafkaListener(topics = { "first-topic", "second-topic" })
    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(
    serviceName = SERVICE_NAME,
    apiName = API_NAME,
    apiVersion = API_VERSION
  )
  static class MergingListener {

    @SnowWhiteInformation(operationId = "receiveOnMergingTopic")
    @KafkaListener(topics = "merging-topic")
    void receive(String payload) {
      // no-op
    }
  }

  static class UnannotatedListener {

    @KafkaListener(topics = "unannotated-topic")
    void receive(String payload) {
      // no-op
    }
  }

  static class NotAListener {

    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(
    serviceName = SERVICE_NAME,
    apiName = "other-api",
    apiVersion = API_VERSION,
    operationId = "receiveOnFirstTopic"
  )
  static class ConflictingListener {

    @KafkaListener(topics = "first-topic")
    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(apiName = API_NAME, apiVersion = API_VERSION)
  static class IncompleteListener {

    @KafkaListener(topics = "incomplete-topic")
    void receiveOnIncompleteTopic(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(serviceName = SERVICE_NAME, apiVersion = API_VERSION)
  static class MissingApiNameListener {

    @KafkaListener(topics = "missing-api-name-topic")
    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(serviceName = SERVICE_NAME, apiName = API_NAME)
  static class MissingApiVersionListener {

    @KafkaListener(topics = "missing-api-version-topic")
    void receive(String payload) {
      // no-op
    }
  }

  @SnowWhiteInformation(
    serviceName = SERVICE_NAME,
    apiName = API_NAME,
    apiVersion = API_VERSION
  )
  static class PlaceholderListener {

    @KafkaListener(topics = "${placeholder.topic}")
    void receive(String payload) {
      // no-op
    }
  }
}
