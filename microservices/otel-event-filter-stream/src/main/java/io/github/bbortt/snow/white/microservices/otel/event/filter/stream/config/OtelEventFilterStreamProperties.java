/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.ConsumerMode.JSON;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.PREFIX;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class OtelEventFilterStreamProperties implements InitializingBean {

  public static final String PREFIX = "snow.white.otel.event.filter";

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

  private final ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final Filtering filtering = new Filtering();

  @Override
  public void afterPropertiesSet() {
    Map<String, String> properties = new HashMap<>();
    properties.put(INBOUND_TOPIC_PROPERTY_NAME, inboundTopicName);
    properties.put(OUTBOUND_TOPIC_PROPERTY_NAME, outboundTopicName);
    properties.put(ApiIndexProperties.BASE_URL_PROPERTY_NAME, apiIndex.baseUrl);

    assertRequiredProperties(properties);
  }

  @Getter
  @Setter
  public static class ApiIndexProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".api-index.base-url";

    private String baseUrl;
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
