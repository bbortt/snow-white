/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka;

import static io.github.bbortt.snow.white.toolkit.spring.kafka.SnowWhiteAutoConfiguration.PROPERTY;
import static io.github.bbortt.snow.white.toolkit.spring.kafka.SnowWhiteAutoConfiguration.PROPERTY_PREFIX;

import io.github.bbortt.snow.white.toolkit.spring.kafka.config.SpringKafkaInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.kafka.interceptor.AsyncApiInformationEnhancer;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Boot applies a single {@link RecordInterceptor} bean to every
 * {@code ConcurrentKafkaListenerContainerFactory} it configures, so no listener-container
 * customizer is needed here.
 * <p>
 * Two situations leave the enhancer unused, both silently: an application that declares a
 * {@code RecordInterceptor} of its own keeps it and we back off, and an application that declares
 * its own {@code ConcurrentKafkaListenerContainerFactory} bypasses Boot's configurer, so nothing
 * attaches the enhancer to it.
 */
@Slf4j
@AutoConfiguration
@Import(SpringKafkaInterceptorProperties.class)
@ConditionalOnProperty(
  prefix = PROPERTY_PREFIX,
  name = PROPERTY,
  havingValue = "true",
  matchIfMissing = true
)
public class SnowWhiteAutoConfiguration {

  static final String PROPERTY_PREFIX =
    "io.github.bbortt.snow.white.toolkit.spring.kafka";
  static final String PROPERTY = "enabled";

  // static: a BeanPostProcessor must be instantiated without initializing its declaring class.
  @Bean
  static KafkaListenerInformationRegistry kafkaListenerInformationRegistry() {
    return new KafkaListenerInformationRegistry();
  }

  // Backing off is the lesser evil: Boot resolves the interceptor with `getIfUnique()`, so adding
  // a second one would leave the application with neither.
  @Bean
  @ConditionalOnMissingBean(RecordInterceptor.class)
  AsyncApiInformationEnhancer asyncApiInformationEnhancer(
    KafkaListenerInformationRegistry kafkaListenerInformationRegistry,
    SpringKafkaInterceptorProperties springKafkaInterceptorProperties
  ) {
    // here rather than in a constructor: the auto configuration loading says nothing about whether
    // the enhancer survived the condition above.
    logger.info("Enhancing OTEL Spans with Snow-White information ✅");

    return new AsyncApiInformationEnhancer(
      kafkaListenerInformationRegistry,
      springKafkaInterceptorProperties
    );
  }
}
