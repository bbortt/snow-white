/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;

class ApiErrorAttributesUnitTest {

  private ApiErrorAttributes fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiErrorAttributes();
  }

  @Nested
  class GetErrorAttributesTest {

    @Test
    void shouldOnlyContainCodeAndMessageKeys() {
      var request = new MockHttpServletRequest();
      request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
      var webRequest = new ServletWebRequest(request);

      var errorAttributes = fixture.getErrorAttributes(
        webRequest,
        ErrorAttributeOptions.defaults()
      );

      assertThat(errorAttributes).containsOnlyKeys("code", "message");
    }

    @Test
    void shouldExcludeSpringBootInternalFields() {
      var request = new MockHttpServletRequest();
      request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 415);
      var webRequest = new ServletWebRequest(request);

      var errorAttributes = fixture.getErrorAttributes(
        webRequest,
        ErrorAttributeOptions.defaults()
      );

      assertThat(errorAttributes).doesNotContainKeys(
        "timestamp",
        "status",
        "error",
        "trace",
        "path"
      );
    }

    @Test
    void shouldMapHttpStatusReasonPhraseToCode() {
      var request = new MockHttpServletRequest();
      request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 415);
      var webRequest = new ServletWebRequest(request);

      var errorAttributes = fixture.getErrorAttributes(
        webRequest,
        ErrorAttributeOptions.defaults()
      );

      assertThat(errorAttributes).containsEntry(
        "code",
        "Unsupported Media Type"
      );
    }

    @Test
    void shouldMapExceptionMessageToMessageField() {
      var request = new MockHttpServletRequest();
      var response = new MockHttpServletResponse();
      request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 415);
      var exception = new HttpMediaTypeNotSupportedException(
        "Content-Type 'null' is not supported."
      );
      fixture.resolveException(request, response, null, exception);
      var webRequest = new ServletWebRequest(request);

      var errorAttributes = fixture.getErrorAttributes(
        webRequest,
        ErrorAttributeOptions.of(Include.MESSAGE)
      );

      assertThat(errorAttributes).containsEntry(
        "message",
        "Content-Type 'null' is not supported."
      );
    }
  }
}
