/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ContentTypeCoverageCalculator.CONTENT_TYPE_HEADER_KEY;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

/**
 * Derives the telemetry attribute keys a single coverage calculation needs, so the active backend
 * can request only those rather than every attribute the instrumented service happened to emit.
 * <p>
 * The set is a fixed enumeration plus one key per header parameter the target spec declares.
 * Nothing keeps that enumeration in sync with the calculators automatically:
 * a calculator added later that reads a key outside this set must have it added here in the same
 * change, or the key will silently be absent from the telemetry it receives.
 */
@Service
@NullMarked
@RequiredArgsConstructor
public class RequiredAttributeKeyService {

  static final String HEADER_KEY_PREFIX = "http.request.header.";

  private static final String HEADER_PARAMETER_LOCATION = "header";

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  /**
   * The keys read by one or more of the coverage calculators, or by
   * {@link OpenApiCoverageService}'s operation-grouping step, regardless of which spec is
   * being measured.
   * <p>
   * {@code url.query} carries every query parameter as one value and
   * {@code url.path} carries the matched path, so neither query nor path parameters contribute a
   * key of their own - only header parameters do.
   */
  @RealizesSw(SwTraceables.SW_021_REQUIRED_ATTRIBUTE_KEY_SET_DERIVATION)
  public Set<String> requiredAttributeKeys(OpenAPI openAPI) {
    Set<String> requiredKeys = new LinkedHashSet<>();

    requiredKeys.add(HTTP_REQUEST_METHOD.getKey());
    requiredKeys.add(URL_PATH.getKey());
    requiredKeys.add(HTTP_RESPONSE_STATUS_CODE.getKey());
    requiredKeys.add(URL_QUERY.getKey());
    requiredKeys.add(CONTENT_TYPE_HEADER_KEY);
    requiredKeys.add(openApiCoverageStreamProperties.getOperationIdAttribute());

    requiredKeys.addAll(headerParameterKeys(openAPI));

    return requiredKeys;
  }

  /**
   * Mirrors {@code ParameterCoverageCalculator}'s own lookup: header parameters are read from the
   * operation they are declared on, and matched against a lower-cased key.
   */
  private static Set<String> headerParameterKeys(OpenAPI openAPI) {
    Set<String> headerKeys = new LinkedHashSet<>();

    if (isNull(openAPI.getPaths())) {
      return headerKeys;
    }

    openAPI
      .getPaths()
      .values()
      .stream()
      .filter(pathItem -> !isNull(pathItem))
      .flatMap(pathItem -> pathItem.readOperations().stream())
      .forEach(operation -> collectHeaderParameterKeys(operation, headerKeys));

    return headerKeys;
  }

  private static void collectHeaderParameterKeys(
    Operation operation,
    Set<String> headerKeys
  ) {
    if (isEmpty(operation.getParameters())) {
      return;
    }

    for (Parameter parameter : operation.getParameters()) {
      if (
        isNull(parameter) ||
        !HEADER_PARAMETER_LOCATION.equals(parameter.getIn()) ||
        isNull(parameter.getName())
      ) {
        continue;
      }

      headerKeys.add(HEADER_KEY_PREFIX + parameter.getName().toLowerCase(ROOT));
    }
  }
}
