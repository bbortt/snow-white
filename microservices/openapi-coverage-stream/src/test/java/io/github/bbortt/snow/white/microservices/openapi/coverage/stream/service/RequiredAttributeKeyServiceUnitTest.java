/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.RequiredAttributeKeyService.HEADER_KEY_PREFIX;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class RequiredAttributeKeyServiceUnitTest {

  private static final String OPERATION_ID_ATTRIBUTE =
    "snow.white.operation.id";

  private static final List<String> FIXED_KEYS = List.of(
    "http.request.method",
    "url.path",
    "http.response.status_code",
    "url.query",
    HEADER_KEY_PREFIX + "content-type",
    OPERATION_ID_ATTRIBUTE
  );

  @Mock
  private OpenApiCoverageStreamProperties openApiCoverageStreamPropertiesMock;

  private RequiredAttributeKeyService fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(OPERATION_ID_ATTRIBUTE)
      .when(openApiCoverageStreamPropertiesMock)
      .getOperationIdAttribute();

    fixture = new RequiredAttributeKeyService(
      openApiCoverageStreamPropertiesMock
    );
  }

  private static OpenAPI openApiWithOperation(Operation operation) {
    var paths = new Paths();
    paths.addPathItem("/foo", new PathItem().get(operation));

    return new OpenAPI().paths(paths);
  }

  private static Parameter parameter(String in, String name) {
    return new Parameter().in(in).name(name);
  }

  @Nested
  class RequiredAttributeKeysTest {

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void returnsFixedKeysOnly_ifSpecDeclaresNoPaths() {
      assertThat(
        fixture.requiredAttributeKeys(new OpenAPI())
      ).containsExactlyElementsOf(FIXED_KEYS);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void addsOneLowerCasedKeyPerHeaderParameter() {
      var openAPI = openApiWithOperation(
        new Operation().parameters(List.of(parameter("header", "X-Api-Key")))
      );

      assertThat(
        fixture.requiredAttributeKeys(openAPI)
      ).containsExactlyElementsOf(
        concat(FIXED_KEYS, HEADER_KEY_PREFIX + "x-api-key")
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void ignoresParametersThatAreNotHeaders() {
      var openAPI = openApiWithOperation(
        new Operation().parameters(
          List.of(
            parameter("query", "page"),
            parameter("path", "id"),
            parameter("cookie", "session")
          )
        )
      );

      assertThat(
        fixture.requiredAttributeKeys(openAPI)
      ).containsExactlyElementsOf(FIXED_KEYS);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void ignoresHeaderParameterWithoutName() {
      var openAPI = openApiWithOperation(
        new Operation().parameters(asList(null, parameter("header", null)))
      );

      assertThat(
        fixture.requiredAttributeKeys(openAPI)
      ).containsExactlyElementsOf(FIXED_KEYS);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void ignoresOperationWithoutParameters() {
      assertThat(
        fixture.requiredAttributeKeys(openApiWithOperation(new Operation()))
      ).containsExactlyElementsOf(FIXED_KEYS);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
    void ignoresNullPathItem() {
      var paths = new Paths();
      paths.addPathItem("/foo", null);

      assertThat(
        fixture.requiredAttributeKeys(new OpenAPI().paths(paths))
      ).containsExactlyElementsOf(FIXED_KEYS);
    }

    private static List<String> concat(
      List<String> keys,
      String additionalKey
    ) {
      return Stream.concat(keys.stream(), Stream.of(additionalKey)).toList();
    }
  }
}
