/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.github.bbortt.snow.white.commons.web.InvalidSortException;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis400Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.WebRequest;

@ExtendWith({ MockitoExtension.class })
class ApiExceptionHandlerUnitTest {

  private ApiExceptionHandler fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiExceptionHandler();
  }

  @Nested
  class HandleExceptionInternalTest {

    @Test
    void shouldBuildErrorBodyWithReasonPhraseForKnownStatus() {
      var exception = new HttpMediaTypeNotSupportedException(
        "Content-Type 'null' is not supported."
      );
      var request = mock(WebRequest.class);

      var response = fixture.handleExceptionInternal(
        exception,
        null,
        new HttpHeaders(),
        UNSUPPORTED_MEDIA_TYPE,
        request
      );

      assertThat(response)
        .isNotNull()
        .extracting(ResponseEntity::getBody)
        .asInstanceOf(type(GetAllApis400Response.class))
        .satisfies(
          e -> assertThat(e.getCode()).isEqualTo("Unsupported Media Type"),
          e ->
            assertThat(e.getMessage()).isEqualTo(
              "Content-Type 'null' is not supported."
            )
        );
    }

    @Test
    void shouldFallBackToStatusCodeString_forUnknownStatus() {
      var exception = new RuntimeException("Custom error");
      var customStatus = HttpStatusCode.valueOf(499);
      var request = mock(WebRequest.class);

      var response = fixture.handleExceptionInternal(
        exception,
        null,
        new HttpHeaders(),
        customStatus,
        request
      );

      assertThat(response)
        .isNotNull()
        .extracting(HttpEntity::getBody)
        .asInstanceOf(type(GetAllApis400Response.class))
        .extracting(GetAllApis400Response::getCode)
        .isEqualTo("499");
    }

    @Test
    void shouldReplaceExistingBody_withErrorDto() {
      var exception = new HttpMediaTypeNotSupportedException("...");
      var request = mock(WebRequest.class);

      var response = fixture.handleExceptionInternal(
        exception,
        "prior body that should be discarded",
        new HttpHeaders(),
        BAD_REQUEST,
        request
      );

      assertThat(response)
        .isNotNull()
        .extracting(HttpEntity::getBody)
        .isInstanceOf(GetAllApis400Response.class);
    }

    @Test
    void shouldPreserveStatusCodeInResponse() {
      var exception = new HttpMediaTypeNotSupportedException("...");
      var request = mock(WebRequest.class);

      var response = fixture.handleExceptionInternal(
        exception,
        null,
        new HttpHeaders(),
        UNSUPPORTED_MEDIA_TYPE,
        request
      );

      assertThat(response)
        .isNotNull()
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void shouldPreserveResponseHeaders() {
      var exception = new HttpMediaTypeNotSupportedException("...");
      var headers = new HttpHeaders();
      headers.setAccept(singletonList(APPLICATION_JSON));
      var request = mock(WebRequest.class);

      var response = fixture.handleExceptionInternal(
        exception,
        null,
        headers,
        UNSUPPORTED_MEDIA_TYPE,
        request
      );

      assertThat(response)
        .isNotNull()
        .extracting(ResponseEntity::getHeaders)
        .isNotNull()
        .extracting(httpHeaders ->
          httpHeaders.containsHeaderValue(ACCEPT, APPLICATION_JSON_VALUE)
        )
        .asInstanceOf(BOOLEAN)
        .isTrue();
    }
  }

  @Nested
  class HandleInvalidSortTest {

    @Test
    void shouldAnswerBadRequestNamingTheAcceptedProperties() {
      var exception = new InvalidSortException(
        "otelServiceName,asc",
        "apiName, apiType, apiVersion, serviceName"
      );
      var request = mock(WebRequest.class);

      var response = fixture.handleInvalidSort(exception, request);

      assertThat(response)
        .isNotNull()
        .satisfies(r -> assertThat(r.getStatusCode()).isEqualTo(BAD_REQUEST))
        .extracting(HttpEntity::getBody)
        .asInstanceOf(type(GetAllApis400Response.class))
        .satisfies(
          e -> assertThat(e.getCode()).isEqualTo("Bad Request"),
          e ->
            assertThat(e.getMessage()).isEqualTo(
              "Cannot sort by 'otelServiceName,asc'. Expected 'property,(asc|desc)' with property one of: apiName, apiType, apiVersion, serviceName."
            )
        );
    }
  }
}
