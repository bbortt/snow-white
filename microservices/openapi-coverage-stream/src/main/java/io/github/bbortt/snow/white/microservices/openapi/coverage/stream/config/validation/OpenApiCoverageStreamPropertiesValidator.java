/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenApiCoverageStreamPropertiesValidator {

  public OpenApiCoverageStreamPropertiesValidator(
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    Map<String, String> fields = new HashMap<>();
    fields.put(
      OpenApiCoverageStreamProperties.ApiIndexProperties.BASE_URL_PROPERTY_NAME,
      openApiCoverageStreamProperties.getApiIndex().getBaseUrl()
    );
    fields.put(
      PREFIX + ".calculation-request-topic",
      openApiCoverageStreamProperties.getCalculationRequestTopic()
    );
    fields.put(
      PREFIX + ".openapi-calculation-response-topic",
      openApiCoverageStreamProperties.getOpenapiCalculationResponseTopic()
    );

    assertRequiredProperties(fields);

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(openApiCoverageStreamProperties)
    );
  }
}
