/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.ConsumerMode.JSON;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class KafkaEventFilterProperties implements InitializingBean {

  public static final String PREFIX = "snow.white.kafka.event.filter";

  public static final String INBOUND_TOPIC_PROPERTY_NAME =
    PREFIX + ".inbound-topic-name";
  private String inboundTopicName;

  public static final String OUTBOUND_TOPIC_PROPERTY_NAME =
    PREFIX + ".outbound-topic-name";
  private String outboundTopicName;

  private Boolean initTopics = false;

  public static final String CONSUMER_MODE_PROPERTY_NAME =
    PREFIX + ".consumer-mode";
  private ConsumerMode consumerMode = JSON;

  private String schemaRegistryUrl;

  private final Filtering filtering = new Filtering();

  @Override
  public void afterPropertiesSet() {
    if (!hasText(inboundTopicName) || !hasText(outboundTopicName)) {
      throw new IllegalArgumentException(
        format(
          "Both '%s' and '%s' must be set!",
          INBOUND_TOPIC_PROPERTY_NAME,
          OUTBOUND_TOPIC_PROPERTY_NAME
        )
      );
    }
  }

  @Getter
  @Setter
  public static class Filtering {

    private static final String DEFAULT_API_NAME_PROPERTY = "api.name";
    private static final String DEFAULT_API_VERSION_PROPERTY = "api.version";
    private static final String DEFAULT_SERVICE_NAME_PROPERTY =
      SERVICE_NAME.getKey();

    private String apiNameProperty = DEFAULT_API_NAME_PROPERTY;
    private String apiVersionProperty = DEFAULT_API_VERSION_PROPERTY;
    private String serviceNameProperty = DEFAULT_SERVICE_NAME_PROPERTY;
  }

  public enum ConsumerMode {
    JSON,
    PROTOBUF,
  }
}
