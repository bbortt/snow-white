/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QualityGateApiPropertiesValidator {

  public QualityGateApiPropertiesValidator(
    QualityGateApiProperties qualityGateApiProperties
  ) {
    Map<String, String> fields = new HashMap<>();
    fields.put(
      PREFIX + ".public-api-gateway-url",
      qualityGateApiProperties.getPublicApiGatewayUrl()
    );

    assertRequiredProperties(fields);

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(qualityGateApiProperties)
    );
  }
}
