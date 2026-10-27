/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka.listener;

import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiName;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiVersion;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveOperationId;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveServiceName;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.aop.framework.AopProxyUtils.ultimateTargetClass;
import static org.springframework.core.MethodIntrospector.selectMethods;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.StringValueResolver;

/**
 * Resolves the {@link SnowWhiteInformation} declared on {@link KafkaListener} methods at startup
 * and keys it by topic, so that the interceptor - which only ever sees a record, never the
 * listener method - can look it up on the hot path.
 */
@Slf4j
@NullMarked
public class KafkaListenerInformationRegistry
  implements BeanPostProcessor, EmbeddedValueResolverAware
{

  private final Map<String, SnowWhiteApiInformation> informationByTopic =
    new ConcurrentHashMap<>();

  private @Nullable StringValueResolver embeddedValueResolver;

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    var targetClass = ultimateTargetClass(bean);
    var classAnnotation = findMergedAnnotation(
      targetClass,
      SnowWhiteInformation.class
    );

    selectMethods(
      targetClass,
      (MetadataLookup<KafkaListener>) method ->
        findMergedAnnotation(method, KafkaListener.class)
    ).forEach((method, kafkaListener) ->
      register(targetClass, method, kafkaListener, classAnnotation)
    );

    return bean;
  }

  public @Nullable SnowWhiteApiInformation findByTopic(String topic) {
    return informationByTopic.get(topic);
  }

  // shortcut: only a single method-level @KafkaListener naming literal or `${...}` `topics()` is
  // matched. Repeated @KafkaListener annotations, class-level @KafkaListener with @KafkaHandler,
  // `topicPattern()`, `topicPartitions()` and `#{...}` SpEL topics all register nothing, so those
  // spans simply stay unstamped - upgrade when a consumer in the wild needs one of them.
  private void register(
    Class<?> targetClass,
    Method method,
    KafkaListener kafkaListener,
    @Nullable SnowWhiteInformation classAnnotation
  ) {
    var methodAnnotation = findMergedAnnotation(
      method,
      SnowWhiteInformation.class
    );

    if (isNull(methodAnnotation) && isNull(classAnnotation)) {
      return;
    }

    var information = new SnowWhiteApiInformation(
      resolveApiName(methodAnnotation, classAnnotation),
      resolveApiVersion(methodAnnotation, classAnnotation),
      resolveServiceName(methodAnnotation, classAnnotation),
      resolveOperationId(methodAnnotation, classAnnotation)
    );

    validate(targetClass, method, information);

    for (var topic : kafkaListener.topics()) {
      var resolvedTopic = resolveEmbeddedValue(topic);

      if (!hasText(resolvedTopic)) {
        continue;
      }

      var previous = informationByTopic.put(resolvedTopic, information);
      if (nonNull(previous) && !previous.equals(information)) {
        logger.warn(
          "Topic [{}] is consumed by more than one @SnowWhiteInformation declaration - spans will carry [{}]",
          resolvedTopic,
          information
        );
      }
    }
  }

  private void validate(
    Class<?> targetClass,
    Method method,
    SnowWhiteApiInformation information
  ) {
    if (
      !hasText(information.serviceName()) ||
      !hasText(information.apiName()) ||
      !hasText(information.apiVersion())
    ) {
      throw new IllegalStateException(
        "Incomplete @SnowWhiteInformation on %s#%s: serviceName='%s', apiName='%s', apiVersion='%s'".formatted(
          targetClass.getSimpleName(),
          method.getName(),
          information.serviceName(),
          information.apiName(),
          information.apiVersion()
        )
      );
    }
  }

  private @Nullable String resolveEmbeddedValue(String topic) {
    return isNull(embeddedValueResolver)
      ? topic
      : embeddedValueResolver.resolveStringValue(topic);
  }

  public record SnowWhiteApiInformation(
    String apiName,
    String apiVersion,
    String serviceName,
    String operationId
  ) {}
}
