/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.HEADER_X_QUALITY_GATE_OWNERSHIP;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.GateOwnershipService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith({ MockitoExtension.class })
class GateOwnershipHeaderFilterUnitTest {

  @Mock
  private GateOwnershipService gateOwnershipServiceMock;

  @Mock
  private FilterChain filterChainMock;

  private MockHttpServletResponse response;

  private GateOwnershipHeaderFilter fixture;

  @BeforeEach
  void beforeEachSetup() {
    response = new MockHttpServletResponse();

    fixture = new GateOwnershipHeaderFilter(gateOwnershipServiceMock);
  }

  @Nested
  class DoFilterInternalTest {

    @ParameterizedTest
    @ValueSource(
      strings = {
        "/api/rest/v1/quality-gates",
        "/api/rest/v1/quality-gates/some-name",
      }
    )
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldPublishOwnership_onGetOfQualityGatePaths(String path)
      throws Exception {
      doReturn(OWNERSHIP_FILE)
        .when(gateOwnershipServiceMock)
        .currentOwnership();

      var request = new MockHttpServletRequest("GET", path);

      fixture.doFilter(request, response, filterChainMock);

      assertThat(
        response.getHeaders(HEADER_X_QUALITY_GATE_OWNERSHIP)
      ).containsExactly(OWNERSHIP_FILE);
      verify(filterChainMock).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "DELETE", "PATCH" })
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldNotPublishOwnership_onWritesToQualityGatePaths(String method)
      throws Exception {
      for (var path : new String[] {
        "/api/rest/v1/quality-gates",
        "/api/rest/v1/quality-gates/some-name",
      }) {
        var writeResponse = new MockHttpServletResponse();
        var request = new MockHttpServletRequest(method, path);

        fixture.doFilter(request, writeResponse, filterChainMock);

        assertThat(
          writeResponse.containsHeader(HEADER_X_QUALITY_GATE_OWNERSHIP)
        ).isFalse();
        verify(filterChainMock).doFilter(request, writeResponse);
      }

      verifyNoInteractions(gateOwnershipServiceMock);
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "/api/rest/v1/openapi-criteria",
        "/api/rest/v1/quality-gates-archive",
        "/api/rest/v1",
        "/",
      }
    )
    @VerifiesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
    void shouldNotPublishOwnership_onGetOfOtherPaths(String path)
      throws Exception {
      var request = new MockHttpServletRequest("GET", path);

      fixture.doFilter(request, response, filterChainMock);

      assertThat(
        response.containsHeader(HEADER_X_QUALITY_GATE_OWNERSHIP)
      ).isFalse();
      verify(filterChainMock).doFilter(request, response);
      verifyNoInteractions(gateOwnershipServiceMock);
    }
  }
}
