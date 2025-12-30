/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@ExtendWith({ MockitoExtension.class })
class RoutingConfigTest {

  @Nested
  class ManagementServerTest {

    @Mock
    private RouteLocatorBuilder routeLocatorBuilderMock;

    @Mock
    private RouteLocatorBuilder.Builder routeLocatorBuilderInnerMock;

    @Mock
    private RouteLocator managementServerMock;

    @BeforeEach
    void beforeEachSetup() {
      doReturn(routeLocatorBuilderInnerMock).when(routeLocatorBuilderMock).routes();
      doReturn(routeLocatorBuilderInnerMock).when(routeLocatorBuilderInnerMock).route(anyString(), any(Function.class));
      doReturn(managementServerMock).when(routeLocatorBuilderInnerMock).build();
    }

    @Test
    void beanShouldBeEnabled_ifServerPortDoesNotEqualManagementServerPort() {
      var contextRunner = new ApplicationContextRunner().withUserConfiguration(RoutingConfig.class);

      contextRunner
        .withBean(ApiGatewayProperties.class, () -> mock(ApiGatewayProperties.class))
        .withBean(RouteLocatorBuilder.class, () -> routeLocatorBuilderMock)
        .withPropertyValues("server.port=8080", "management.server.port=8090")
        .run(context ->
          assertThat(context)
            .asInstanceOf(type(AssertableApplicationContext.class))
            .satisfies(c -> assertThat(c).getBean("managementServer", RouteLocator.class).isEqualTo(managementServerMock))
        );

      verify(routeLocatorBuilderInnerMock).route(eq("info-endpoint"), any(Function.class));
    }

    @Test
    void beanShouldBeDisabled_ifServerPortIsAlsoManagementServerPort() {
      var contextRunner = new ApplicationContextRunner().withUserConfiguration(RoutingConfig.class);

      contextRunner
        .withBean(ApiGatewayProperties.class, () -> mock(ApiGatewayProperties.class))
        .withBean(RouteLocatorBuilder.class, () -> routeLocatorBuilderMock)
        .run(context ->
          assertThat(context)
            .asInstanceOf(type(AssertableApplicationContext.class))
            .satisfies(c -> assertThat(c).doesNotHaveBean("managementServer"))
        );

      verify(routeLocatorBuilderInnerMock, never()).route(eq("info-endpoint"), any(Function.class));
    }
  }
}
