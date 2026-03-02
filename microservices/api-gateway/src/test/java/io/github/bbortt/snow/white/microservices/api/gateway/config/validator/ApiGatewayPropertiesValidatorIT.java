/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config.validation;

import io.github.bbortt.snow.white.microservices.api.gateway.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ApiGatewayPropertiesValidatorIT {

    @Autowired
    private ApiGatewayPropertiesValidator apiGatewayPropertiesValidator;

    @Test
    void shouldBeRegisteredWithinSpringContext() {
        assertThat(apiGatewayPropertiesValidator).isNotNull();
    }
}