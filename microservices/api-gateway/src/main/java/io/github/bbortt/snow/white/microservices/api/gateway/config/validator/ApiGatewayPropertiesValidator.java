/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config.validation;

import io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties.PREFIX;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class ApiGatewayPropertiesValidator {

    public ApiGatewayPropertiesValidator(ApiGatewayProperties apiGatewayProperties, Environment environment) {
        Map<String, String> fields = new HashMap<>();
        fields.put(PREFIX + ".quality-gate-api-url", apiGatewayProperties.getQualityGateApiUrl());
        fields.put(PREFIX + ".report-coordinator-api-url", apiGatewayProperties.getReportCoordinatorApiUrl());

        if (nonNull(environment) && environment.acceptsProfiles(Profiles.of("prod"))) {
            fields.put(PREFIX + ".public-url", apiGatewayProperties.getPublicUrl());
        }

        assertRequiredProperties(fields);

        logger.info("Configuration: {}",  toMaskedJsonRepresentation(apiGatewayProperties));
    }
}
