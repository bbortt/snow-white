/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.mcp.api.config.McpApiProperties.ApiIndexProperties;
import static io.github.bbortt.snow.white.microservices.mcp.api.config.McpApiProperties.CriteriaProperties;
import static io.github.bbortt.snow.white.microservices.mcp.api.config.McpApiProperties.ReportProperties;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Without this, an unset base URL leaves the server answering every tool call with a
 * connection error against a half-formed address. A stdio process is launched by an agent
 * client that shows none of that, so the misconfiguration has to refuse to start.
 */
@Component
public class McpApiPropertiesValidator {

  public McpApiPropertiesValidator(McpApiProperties mcpApiProperties) {
    Map<String, String> properties = new HashMap<>();
    properties.put(
      ApiIndexProperties.BASE_URL_PROPERTY_NAME,
      mcpApiProperties.getApiIndex().getBaseUrl()
    );
    properties.put(
      CriteriaProperties.BASE_URL_PROPERTY_NAME,
      mcpApiProperties.getCriteria().getBaseUrl()
    );
    properties.put(
      ReportProperties.BASE_URL_PROPERTY_NAME,
      mcpApiProperties.getReport().getBaseUrl()
    );

    assertRequiredProperties(properties);
  }
}
