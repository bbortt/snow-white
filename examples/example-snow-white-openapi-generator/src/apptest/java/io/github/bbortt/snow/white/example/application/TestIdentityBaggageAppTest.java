/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.springframework.http.HttpStatus.OK;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpEndpointConfiguration;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves that a caller can name the test behind a request without the service taking part in it -
 * here for a service whose API is produced by the Snow-White OpenAPI generator.
 * <p>
 * This is the same claim {@code examples/example-spring-boot} makes, deliberately repeated against
 * the generated story: the hand-written example could always be read as "the author simply did not
 * add any code", whereas here the request-handling layer is generated and the generator contributes
 * nothing to the copy either. The convention costs a consuming service no Snow-White dependency and
 * no code, generated or otherwise.
 * <p>
 * The example application is instrumented by the OpenTelemetry Java agent and its image sets
 * {@code otel.java.experimental.span-attributes.copy-from-baggage.include=test.case.name} (see the
 * {@code Dockerfile}) - nothing else. This test drives the running container over HTTP and reads
 * what it exported: the agent ships OTLP/HTTP to a collector, which re-encodes it as OTLP/JSON into
 * WireMock, whose request journal is where the assertions look (see
 * {@code src/apptest/resources/docker-compose-apptest.yaml}).
 * <p>
 * The collector runs without processors on purpose: a batch or attribute step could plausibly add or
 * drop the very attribute under test, and the claim being made is about what the application
 * exported.
 */
@CitrusSupport
class TestIdentityBaggageAppTest {

  private static final String TEST_CASE_NAME_ATTRIBUTE = "test.case.name";
  private static final String OTLP_TRACES_PATH = "/v1/traces";

  /**
   * OTLP's {@code SPAN_KIND_SERVER}. The attribute has to land on the span the service created for
   * the incoming request, which is the only one this example produces at all.
   */
  private static final int SPAN_KIND_SERVER = 2;

  /**
   * Generous on purpose: an export has to be batched by the agent, forwarded by the collector and
   * journalled by WireMock before it can be read, and none of those steps is instantaneous on a
   * loaded CI runner.
   */
  private static final Duration EXPORT_TIMEOUT = ofSeconds(30);

  @BindToRegistry
  private final HttpClient exampleApplication = exampleApplication();

  @BeforeAll
  static void beforeAllSetup() {
    configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );

    // Answering the collector keeps its export queue draining; the response body is irrelevant,
    // the recorded request is the point.
    stubFor(post(urlEqualTo(OTLP_TRACES_PATH)).willReturn(okJson("{}")));
  }

  /**
   * A request carrying {@code baggage: test.case.name=<name>} must produce a server span carrying
   * {@code test.case.name} with exactly that value.
   * <p>
   * The header also carries a second entry that is not configured for copying, so the test states
   * the boundary as well: the configured key is copied, the rest of the baggage is not.
   */
  @Test
  @CitrusTest
  @VerifiesArch(ArchTraceables.ARCH_013_TEST_IDENTITY_TRAVELS_AS_BAGGAGE)
  void shouldCopyTestCaseNameBaggageOntoTheServerSpan(
    @CitrusResource TestActionRunner runner
  ) {
    var marker = randomUUID().toString();
    var testCaseName =
      "io.github.bbortt.snow.white.example.application.TestIdentityBaggageAppTest#shouldCopyTestCaseNameBaggageOntoTheServerSpan";

    runner.run(
      http()
        .client(exampleApplication)
        .send()
        .get("/ping")
        .message()
        .queryParam("message", marker)
        .header(
          "baggage",
          format("%s=%s,team=platform", TEST_CASE_NAME_ATTRIBUTE, testCaseName)
        )
    );
    runner.run(http().client(exampleApplication).receive().response(OK));

    var serverSpan = awaitExportedServerSpan(marker);

    assertThat(stringAttribute(serverSpan, TEST_CASE_NAME_ATTRIBUTE)).isEqualTo(
      testCaseName
    );
    assertThat(stringAttribute(serverSpan, "team")).isNull();
  }

  /**
   * A request without the header must produce the same span without the attribute - the identity is
   * something a caller opts into, and a caller that does not is served exactly as before.
   * <p>
   * The span is asserted to be the right one by an attribute the agent always records, so the
   * absence being claimed is an absence on a span that was found rather than a span that was
   * missed.
   */
  @Test
  @CitrusTest
  @VerifiesArch(ArchTraceables.ARCH_013_TEST_IDENTITY_TRAVELS_AS_BAGGAGE)
  void shouldExportTheServerSpanWithoutTestCaseName_whenNoBaggageIsSent(
    @CitrusResource TestActionRunner runner
  ) {
    var marker = randomUUID().toString();

    runner.run(
      http()
        .client(exampleApplication)
        .send()
        .get("/ping")
        .message()
        .queryParam("message", marker)
    );
    runner.run(http().client(exampleApplication).receive().response(OK));

    var serverSpan = awaitExportedServerSpan(marker);

    assertThat(stringAttribute(serverSpan, "http.request.method")).isEqualTo(
      "GET"
    );
    assertThat(stringAttribute(serverSpan, TEST_CASE_NAME_ATTRIBUTE)).isNull();
  }

  /**
   * Waits for the server span belonging to one request, identified by the random marker that
   * request sent as its {@code message} query parameter.
   * <p>
   * Every test has its own marker, so the journal is never reset and nothing here depends on test
   * order or on which spans another test left behind.
   */
  private JsonNode awaitExportedServerSpan(String marker) {
    return await()
      .alias("exported server span for message=" + marker)
      .atMost(EXPORT_TIMEOUT)
      .pollInterval(ofSeconds(1))
      .until(() -> findExportedServerSpan(marker), Objects::nonNull);
  }

  private JsonNode findExportedServerSpan(String marker) {
    return findAll(postRequestedFor(urlEqualTo(OTLP_TRACES_PATH)))
      .stream()
      .map(LoggedRequest::getBodyAsString)
      .map(JsonMapper.shared()::readTree)
      .flatMap(export -> elements(export.path("resourceSpans")))
      .flatMap(resourceSpans -> elements(resourceSpans.path("scopeSpans")))
      .flatMap(scopeSpans -> elements(scopeSpans.path("spans")))
      .filter(span -> span.path("kind").asInt() == SPAN_KIND_SERVER)
      .filter(span ->
        ("message=" + marker).equals(stringAttribute(span, "url.query"))
      )
      .findFirst()
      .orElse(null);
  }

  private static String stringAttribute(JsonNode span, String key) {
    return elements(span.path("attributes"))
      .filter(attribute -> key.equals(attribute.path("key").asString()))
      .map(attribute -> attribute.path("value").path("stringValue").asString())
      .findFirst()
      .orElse(null);
  }

  private static Stream<JsonNode> elements(JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false);
  }

  private static HttpClient exampleApplication() {
    var endpointConfiguration = new HttpEndpointConfiguration();
    endpointConfiguration.setRequestUrl(
      format(
        "http://%s:%s",
        getProperty("example-snow-white-openapi-generator.host", "localhost"),
        parseInt(
          getProperty("example-snow-white-openapi-generator.port", "18080")
        )
      )
    );
    return new HttpClient(endpointConfiguration);
  }
}
