/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.web.InvalidSortException;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate400Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@NullMarked
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
    Exception ex,
    @Nullable Object body,
    HttpHeaders headers,
    HttpStatusCode statusCode,
    WebRequest request
  ) {
    var resolved = HttpStatus.resolve(statusCode.value());
    var error = CalculateQualityGate400Response.builder()
      .code(
        resolved != null
          ? resolved.getReasonPhrase()
          : String.valueOf(statusCode.value())
      )
      .message(ex.getMessage())
      .build();
    return super.handleExceptionInternal(
      ex,
      error,
      headers,
      statusCode,
      request
    );
  }

  /**
   * A {@code sort} the listing cannot honour is the caller's mistake, not the server's.
   *
   * <p>Routed through {@link #handleExceptionInternal} so the response carries the same
   * {@code {code, message}} body as every other error here, with the exception's message — which
   * names the properties this endpoint accepts — as the {@code message}. Unmapped, this reached the
   * default {@code 500}.
   */
  @RealizesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
  @ExceptionHandler(InvalidSortException.class)
  @Nullable
  ResponseEntity<Object> handleInvalidSort(
    InvalidSortException ex,
    WebRequest request
  ) {
    return handleExceptionInternal(
      ex,
      null,
      new HttpHeaders(),
      BAD_REQUEST,
      request
    );
  }
}
