/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.toolkit.spring.kafka.config.SpringKafkaInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.kafka.interceptor.AsyncApiInformationEnhancer;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

class SnowWhiteAutoConfigurationIT {

  private final ApplicationContextRunner contextRunner =
    new ApplicationContextRunner().withConfiguration(
      AutoConfigurations.of(
        ConfigurationPropertiesAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        SnowWhiteAutoConfiguration.class
      )
    );

  @Test
  void beansAreLoaded() {
    contextRunner.run(context ->
      assertThat(context)
        .hasSingleBean(SpringKafkaInterceptorProperties.class)
        .hasSingleBean(AsyncApiInformationEnhancer.class)
        .hasSingleBean(KafkaListenerInformationRegistry.class)
    );
  }

  @Test
  void bootAppliesTheEnhancerToTheListenerContainerFactory() {
    contextRunner.run(context ->
      assertThat(
        context
          .getBean(ConcurrentKafkaListenerContainerFactory.class)
          .getRecordInterceptor()
      ).isSameAs(context.getBean(AsyncApiInformationEnhancer.class))
    );
  }

  @Test
  void autoConfigurationCanBeDisabled() {
    contextRunner
      .withPropertyValues(
        "io.github.bbortt.snow.white.toolkit.spring.kafka.enabled=false"
      )
      .run(context ->
        assertThat(context)
          .doesNotHaveBean(AsyncApiInformationEnhancer.class)
          // the registry is the part that can fail a consumer's startup, so the kill switch has to
          // reach it too.
          .doesNotHaveBean(KafkaListenerInformationRegistry.class)
      );
  }

  @Test
  void enhancerBacksOffWhenTheApplicationDeclaresItsOwnRecordInterceptor() {
    contextRunner
      .withUserConfiguration(RecordInterceptorConfiguration.class)
      .run(context -> {
        assertThat(context).doesNotHaveBean(AsyncApiInformationEnhancer.class);
        assertThat(
          context
            .getBean(ConcurrentKafkaListenerContainerFactory.class)
            .getRecordInterceptor()
        ).isSameAs(context.getBean(RecordInterceptor.class));
      });
  }

  @Test
  void attributeNamesAreConfigurable() {
    contextRunner
      .withPropertyValues(
        "io.github.bbortt.snow.white.toolkit.spring.kafka.operation-id-attribute=custom.operation.id"
      )
      .run(context ->
        assertThat(
          context
            .getBean(SpringKafkaInterceptorProperties.class)
            .getOperationIdAttribute()
        ).isEqualTo("custom.operation.id")
      );
  }

  @Configuration(proxyBeanMethods = false)
  static class RecordInterceptorConfiguration {

    @Bean
    RecordInterceptor<Object, Object> recordInterceptor() {
      return (record, consumer) -> record;
    }
  }
}
