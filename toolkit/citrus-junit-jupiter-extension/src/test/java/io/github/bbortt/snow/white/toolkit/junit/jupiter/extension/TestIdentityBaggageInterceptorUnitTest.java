/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.BAGGAGE_HEADER;
import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.TEST_CASE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

class TestIdentityBaggageInterceptorUnitTest {

  private static final String AN_IDENTITY =
    "com.example.ExampleIT#shouldDoThing";
  private static final byte[] NO_BODY = new byte[0];

  private TestIdentityBaggageInterceptor fixture;
  private MockClientHttpRequest request;
  private ClientHttpRequestExecution execution;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new TestIdentityBaggageInterceptor();
    request = new MockClientHttpRequest();
    execution = mock(ClientHttpRequestExecution.class);
  }

  private void intercept() throws IOException {
    fixture.intercept(request, NO_BODY, execution);
  }

  private void interceptWithIdentity(String identity) throws IOException {
    var baggage = Baggage.builder().put(TEST_CASE_NAME, identity).build();

    try (var _ = baggage.storeInContext(Context.current()).makeCurrent()) {
      intercept();
    }
  }

  private String baggageHeader() {
    return request.getHeaders().getFirst(BAGGAGE_HEADER);
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldAddTheCurrentIdentityAsABaggageHeader() throws IOException {
    interceptWithIdentity(AN_IDENTITY);

    assertThat(baggageHeader())
      .isNotNull()
      .startsWith(TEST_CASE_NAME + "=")
      .contains("com.example.ExampleIT");
  }

  /**
   * Wiring it unconditionally has to be safe, including on a client used outside a test - otherwise
   * the "wire it once" promise would come with a caveat.
   */
  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldAddNoHeaderWithoutACurrentIdentity() throws IOException {
    intercept();

    assertThat(request.getHeaders().headerNames()).doesNotContain(
      BAGGAGE_HEADER
    );
  }

  /**
   * The caller's own baggage is not this interceptor's to discard; W3C baggage is a comma-separated
   * list precisely so several concerns can share the header.
   */
  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldAppendToABaggageHeaderTheRequestAlreadyCarries()
    throws IOException {
    request.getHeaders().set(BAGGAGE_HEADER, "team=platform");

    interceptWithIdentity(AN_IDENTITY);

    assertThat(baggageHeader())
      .startsWith("team=platform,")
      .contains(TEST_CASE_NAME + "=");
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldReplaceABlankBaggageHeaderRatherThanAppendToIt()
    throws IOException {
    request.getHeaders().set(BAGGAGE_HEADER, "  ");

    interceptWithIdentity(AN_IDENTITY);

    assertThat(baggageHeader()).doesNotStartWith(",").doesNotStartWith(" ");
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldAlwaysExecuteTheRequest() throws IOException {
    var response = mock(ClientHttpResponse.class);
    when(execution.execute(any(), any())).thenReturn(response);

    interceptWithIdentity(AN_IDENTITY);

    verify(execution).execute(request, NO_BODY);
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldExecuteTheRequestEvenWithoutAnIdentity() throws IOException {
    intercept();

    verify(execution).execute(request, NO_BODY);
  }

  /**
   * Stated so the one-line wiring in each {@code CitrusUtils} keeps meaning what it says: a second
   * interceptor's header must survive, which it only does if this one reads the headers it is given
   * rather than a cached copy.
   */
  @Test
  @VerifiesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
  void shouldLeaveOtherHeadersAlone() throws IOException {
    request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");

    interceptWithIdentity(AN_IDENTITY);

    assertThat(request.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo(
      "application/json"
    );
  }
}
