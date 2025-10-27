/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.PREFIX;

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
public class ReportCoordinationServiceProperties implements InitializingBean {

  public static final String PREFIX = "snow.white.report.coordinator.api";

  private String calculationRequestTopic;

  private Boolean initTopics = false;

  private String publicApiGatewayUrl;

  private String qualityGateApiUrl;

  private final OpenapiCalculationResponse openapiCalculationResponse =
    new OpenapiCalculationResponse();

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(PREFIX + ".calculation-request-topic", calculationRequestTopic);
    fields.put(
      OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC,
      openapiCalculationResponse.getTopic()
    );
    fields.put(PREFIX + ".public-api-gateway-url", publicApiGatewayUrl);
    fields.put(PREFIX + ".quality-gate-api-url", qualityGateApiUrl);

    assertRequiredProperties(fields);
  }

  @Getter
  @Setter
  public static class OpenapiCalculationResponse {

    private static final String OPENAPI_CALCULATION_RESPONSE =
      PREFIX + ".openapi-calculation-response";

    public static final String CONSUMER_GROUP_ID =
      OPENAPI_CALCULATION_RESPONSE + ".consumer-group-id";
    public static final String OPENAPI_CALCULATION_RESPONSE_TOPIC =
      OPENAPI_CALCULATION_RESPONSE + ".topic";

    public static final String DEFAULT_CONSUMER_GROUP_ID =
      "report-coordinator-api";

    private String consumerGroupId = DEFAULT_CONSUMER_GROUP_ID;
    private String topic;
  }
}
