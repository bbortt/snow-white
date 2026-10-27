/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class McpApiPropertiesValidatorUnitTest {

  private static McpApiProperties allBaseUrlsSet() {
    var properties = new McpApiProperties();
    properties.getApiIndex().setBaseUrl("http://api-index");
    properties.getCriteria().setBaseUrl("http://quality-gate");
    properties.getReport().setBaseUrl("http://report-coordinator");
    return properties;
  }

  /** Each entry clears one base URL, named by the property the message must report. */
  static Stream<Object[]> oneBaseUrlMissing() {
    return Stream.of(
      new Object[] {
        McpApiProperties.ApiIndexProperties.BASE_URL_PROPERTY_NAME,
        (Consumer<McpApiProperties>) properties ->
          properties.getApiIndex().setBaseUrl(null),
      },
      new Object[] {
        McpApiProperties.CriteriaProperties.BASE_URL_PROPERTY_NAME,
        (Consumer<McpApiProperties>) properties ->
          properties.getCriteria().setBaseUrl("  "),
      },
      new Object[] {
        McpApiProperties.ReportProperties.BASE_URL_PROPERTY_NAME,
        (Consumer<McpApiProperties>) properties ->
          properties.getReport().setBaseUrl(""),
      }
    );
  }

  @Test
  void acceptsAConfigurationWithEveryBaseUrlSet() {
    assertThatNoException().isThrownBy(() ->
      new McpApiPropertiesValidator(allBaseUrlsSet())
    );
  }

  @ParameterizedTest
  @MethodSource("oneBaseUrlMissing")
  void refusesToStartNamingTheBaseUrlThatIsMissing(
    String propertyName,
    Consumer<McpApiProperties> clear
  ) {
    var properties = allBaseUrlsSet();
    clear.accept(properties);

    assertThatThrownBy(() -> new McpApiPropertiesValidator(properties))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "All properties must be configured - missing: [%s].".formatted(
          propertyName
        )
      );
  }
}
