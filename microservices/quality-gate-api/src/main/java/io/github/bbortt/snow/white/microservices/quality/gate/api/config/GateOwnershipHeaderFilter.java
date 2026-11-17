/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.HEADER_X_QUALITY_GATE_OWNERSHIP;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.QualityGateApi.PATH_GET_ALL_QUALITY_GATES;
import static org.springframework.http.HttpMethod.GET;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.GateOwnershipService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Publishes the instance's gate-ownership mode on every response to a gate read, so a client learns
 * whether a write would be refused before it offers the control that issues one.
 *
 * <p>A filter rather than a line in each resource method, because "every response" includes the
 * ones the resource never returns itself — the {@code 400} a malformed {@code sort} earns is
 * produced by the exception handler, and a client reasoning about a read it just made should not
 * have to care which code path answered it.
 *
 * <p>Deliberately scoped to the two gate reads. The criteria catalog is served from a different
 * path and the mode is not a fact about it; the writes publish nothing, because their own refusal
 * already names the mode.
 */
@Component
@NullMarked
@RequiredArgsConstructor
public class GateOwnershipHeaderFilter extends OncePerRequestFilter {

  // The path belongs to the spec, not to this filter: take it from the generated API.
  private static final String QUALITY_GATES_PATH = PATH_GET_ALL_QUALITY_GATES;

  private final GateOwnershipService gateOwnershipService;

  @RealizesSw(SwTraceables.SW_053_MANAGED_MODE_PUBLISHED_ON_GATE_READS)
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    if (isQualityGateRead(request)) {
      response.setHeader(
        HEADER_X_QUALITY_GATE_OWNERSHIP,
        gateOwnershipService.currentOwnership()
      );
    }

    filterChain.doFilter(request, response);
  }

  private static boolean isQualityGateRead(HttpServletRequest request) {
    if (!GET.matches(request.getMethod())) {
      return false;
    }

    var path = request.getRequestURI();

    return (
      QUALITY_GATES_PATH.equals(path) ||
      path.startsWith(QUALITY_GATES_PATH + "/")
    );
  }
}
