/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.OUTBOUND_TOPIC_PROPERTY_NAME;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtelEventFilterStreamPropertiesValidator {

  public OtelEventFilterStreamPropertiesValidator(
    OtelEventFilterStreamProperties otelEventFilterStreamProperties
  ) {
    Map<String, String> properties = new HashMap<>();
    properties.put(
      INBOUND_TOPIC_PROPERTY_NAME,
      otelEventFilterStreamProperties.getInboundTopicName()
    );
    properties.put(
      OUTBOUND_TOPIC_PROPERTY_NAME,
      otelEventFilterStreamProperties.getOutboundTopicName()
    );
    properties.put(
      OtelEventFilterStreamProperties.ApiIndexProperties.BASE_URL_PROPERTY_NAME,
      otelEventFilterStreamProperties.getApiIndex().getBaseUrl()
    );

    assertRequiredProperties(properties);

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(otelEventFilterStreamProperties)
    );
  }
}
