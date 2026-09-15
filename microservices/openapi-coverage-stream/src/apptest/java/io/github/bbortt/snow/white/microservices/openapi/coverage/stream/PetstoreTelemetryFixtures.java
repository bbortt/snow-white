/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.OtlpTraceFixtures.Span.span;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.NoArgsConstructor;

/**
 * Realistic-looking telemetry for {@code openapi/petstore-api.yaml}, shared between
 * {@link OpenApiCoverageStreamCrossBackendAppTest}'s scenarios and, via {@link #randomTraffic},
 * any future load test - both need the same "what does a real request against this API look
 * like" data, just at different volumes.
 */
@NoArgsConstructor(access = PRIVATE)
final class PetstoreTelemetryFixtures {

  private static final List<String> PET_NAMES = List.of(
    "Bella",
    "Max",
    "Charlie",
    "Luna",
    "Cooper",
    "Daisy",
    "Rocky",
    "Milo"
  );

  private static final List<String> PET_STATUSES = List.of(
    "available",
    "pending",
    "sold"
  );

  private static final List<String> USERNAMES = List.of(
    "jdoe",
    "asmith",
    "mgarcia",
    "kwilliams"
  );

  /**
   * One representative, successful span per operation documented in {@code petstore-api.yaml} -
   * the happy-path traffic a freshly-deployed, well-behaved API client would generate.
   */
  static List<OtlpTraceFixtures.Span> standardOperationSpans(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    List<OtlpTraceFixtures.Span> spans = new ArrayList<>();

    spans.add(
      operation(serviceName, apiName, apiVersion, "POST", "/pet", 200L)
        .attribute("http.request.header.content-type", "application/json")
        .attribute("pet.name", PET_NAMES.get(0))
    );
    spans.add(
      operation(serviceName, apiName, apiVersion, "PUT", "/pet", 200L)
        .attribute("http.request.header.content-type", "application/json")
        .attribute("pet.name", PET_NAMES.get(1))
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/pet/findByStatus",
        200L
      ).attribute(URL_QUERY.getKey(), "status=available&limit=20")
    );
    spans.add(
      operation(serviceName, apiName, apiVersion, "GET", "/pet/123", 200L)
    );
    spans.add(
      operation(serviceName, apiName, apiVersion, "DELETE", "/pet/123", 200L)
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "POST",
        "/store/order",
        200L
      ).attribute("http.request.header.content-type", "application/json")
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/store/order/456",
        200L
      )
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/store/inventory",
        200L
      )
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "POST",
        "/user",
        200L
      ).attribute("http.request.header.content-type", "application/json")
    );
    spans.add(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/user/login",
        200L
      ).attribute(URL_QUERY.getKey(), "username=jdoe&password=hunter2")
    );

    return spans;
  }

  /**
   * The documented error branches {@link #standardOperationSpans} never touches - a pet lookup
   * and an order lookup that both miss, each with the API's {@code ApiResponse} error shape
   * (required {@code code}/{@code message} fields), exercising
   * {@code RESPONSE_CODE_COVERAGE}/{@code ERROR_RESPONSE_CODE_COVERAGE}/
   * {@code REQUIRED_ERROR_FIELDS_COVERAGE} beyond the happy path.
   */
  static List<OtlpTraceFixtures.Span> errorScenarioSpans(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    return List.of(
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/pet/999999",
        404L
      ).attribute("http.request.header.content-type", "application/json"),
      operation(
        serviceName,
        apiName,
        apiVersion,
        "GET",
        "/store/order/999999",
        404L
      ).attribute("http.request.header.content-type", "application/json")
    );
  }

  /**
   * Randomized, realistic-shaped traffic across every documented operation - deterministic for a
   * given seed, so a load test built on this stays reproducible run to run. Kept here rather than
   * in a load-test module because it needs the same operation catalogue and realistic value pools
   * as the functional fixtures above, and a load test is exactly "the functional scenario, at
   * volume".
   */
  static List<OtlpTraceFixtures.Span> randomTraffic(
    String serviceName,
    String apiName,
    String apiVersion,
    int count,
    long seed
  ) {
    var random = new Random(seed);
    List<OtlpTraceFixtures.Span> spans = new ArrayList<>();

    for (var i = 0; i < count; i++) {
      spans.add(
        switch (random.nextInt(6)) {
          case 0 -> operation(
            serviceName,
            apiName,
            apiVersion,
            "POST",
            "/pet",
            200L
          )
            .attribute("http.request.header.content-type", "application/json")
            .attribute("pet.name", randomPetName(random));
          case 1 -> operation(
            serviceName,
            apiName,
            apiVersion,
            "GET",
            "/pet/findByStatus",
            200L
          ).attribute(URL_QUERY.getKey(), "status=" + randomPetStatus(random));
          case 2 -> operation(
            serviceName,
            apiName,
            apiVersion,
            "GET",
            "/pet/" + randomId(random),
            random.nextInt(10) == 0 ? 404L : 200L
          );
          case 3 -> operation(
            serviceName,
            apiName,
            apiVersion,
            "POST",
            "/store/order",
            200L
          ).attribute("http.request.header.content-type", "application/json");
          case 4 -> operation(
            serviceName,
            apiName,
            apiVersion,
            "GET",
            "/store/order/" + randomId(random),
            random.nextInt(10) == 0 ? 404L : 200L
          );
          default -> operation(
            serviceName,
            apiName,
            apiVersion,
            "GET",
            "/user/login",
            200L
          ).attribute(
            URL_QUERY.getKey(),
            "username=" + randomUsername(random) + "&password=hunter2"
          );
        }
      );
    }

    return spans;
  }

  private static String randomPetStatus(Random random) {
    return PET_STATUSES.get(random.nextInt(PET_STATUSES.size()));
  }

  private static String randomUsername(Random random) {
    return USERNAMES.get(random.nextInt(USERNAMES.size()));
  }

  private static String randomPetName(Random random) {
    return PET_NAMES.get(random.nextInt(PET_NAMES.size()));
  }

  private static long randomId(Random random) {
    return 100L + random.nextInt(900);
  }

  private static OtlpTraceFixtures.Span operation(
    String serviceName,
    String apiName,
    String apiVersion,
    String method,
    String path,
    long statusCode
  ) {
    return span(serviceName, method + " " + path)
      .attribute(HTTP_REQUEST_METHOD.getKey(), method)
      .attribute(URL_PATH.getKey(), path)
      .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), statusCode)
      .attribute("api.name", apiName)
      .attribute("api.version", apiVersion);
  }
}
