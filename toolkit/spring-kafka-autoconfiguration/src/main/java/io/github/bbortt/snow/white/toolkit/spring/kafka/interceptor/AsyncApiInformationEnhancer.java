/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka.interceptor;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.spring.kafka.config.SpringKafkaInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.NullMarked;
import org.springframework.kafka.listener.RecordInterceptor;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class AsyncApiInformationEnhancer
  implements RecordInterceptor<Object, Object>
{

  private final SpanProvider spanProvider = new SpanProvider();

  private final KafkaListenerInformationRegistry kafkaListenerInformationRegistry;

  private final SpringKafkaInterceptorProperties springKafkaInterceptorProperties;

  @Override
  public ConsumerRecord<Object, Object> intercept(
    ConsumerRecord<Object, Object> consumerRecord,
    Consumer<Object, Object> consumer
  ) {
    var currentSpan = spanProvider.getCurrentSpan();
    var information = kafkaListenerInformationRegistry.findByTopic(
      consumerRecord.topic()
    );

    if (isNull(currentSpan) || isNull(information)) {
      return consumerRecord;
    }

    logger.trace(
      "Enhancing span: topic=[{}], information=[{}]",
      consumerRecord.topic(),
      information
    );

    currentSpan.setAttribute(
      springKafkaInterceptorProperties.getApiNameAttribute(),
      information.apiName()
    );
    currentSpan.setAttribute(
      springKafkaInterceptorProperties.getApiVersionAttribute(),
      information.apiVersion()
    );

    if (
      hasText(springKafkaInterceptorProperties.getOtelServiceNameAttribute())
    ) {
      currentSpan.setAttribute(
        springKafkaInterceptorProperties.getOtelServiceNameAttribute(),
        information.serviceName()
      );
    }

    if (hasText(information.operationId())) {
      currentSpan.setAttribute(
        springKafkaInterceptorProperties.getOperationIdAttribute(),
        information.operationId()
      );
    }

    return consumerRecord;
  }
}
