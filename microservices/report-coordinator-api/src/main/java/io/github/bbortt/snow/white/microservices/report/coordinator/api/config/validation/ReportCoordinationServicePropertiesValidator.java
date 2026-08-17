/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportCoordinationServicePropertiesValidator {

  public ReportCoordinationServicePropertiesValidator(
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    Map<String, String> fields = new HashMap<>();
    fields.put(
      PREFIX + ".calculation-request-topic",
      reportCoordinationServiceProperties.getCalculationRequestTopic()
    );
    fields.put(
      ReportCoordinationServiceProperties.OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC,
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic()
    );
    fields.put(
      PREFIX + ".public-api-gateway-url",
      reportCoordinationServiceProperties.getPublicApiGatewayUrl()
    );
    fields.put(
      ReportCoordinationServiceProperties.ApiIndexProperties.BASE_URL_PROPERTY_NAME,
      reportCoordinationServiceProperties.getApiIndex().getBaseUrl()
    );
    fields.put(
      ReportCoordinationServiceProperties.QualityGateApiProperties.BASE_URL_PROPERTY_NAME,
      reportCoordinationServiceProperties.getQualityGateApi().getBaseUrl()
    );

    assertRequiredProperties(fields);

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(reportCoordinationServiceProperties)
    );
  }
}
