/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.api.index.config.ApiIndexProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.api.index.config.ApiIndexProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiIndexPropertiesValidator {

  public ApiIndexPropertiesValidator(ApiIndexProperties apiIndexProperties) {
    Map<String, String> fields = new HashMap<>();
    fields.put(
      PREFIX + ".public-api-gateway-url",
      apiIndexProperties.getPublicApiGatewayUrl()
    );

    assertRequiredProperties(fields);

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(apiIndexProperties)
    );
  }
}
