/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
    Exception ex,
    Object body,
    HttpHeaders headers,
    HttpStatusCode statusCode,
    WebRequest request
  ) {
    var resolved = HttpStatus.resolve(statusCode.value());
    var error = Error.builder()
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
}
