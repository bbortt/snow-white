/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.web.filter;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpaWebFilterTestController {

  public static final String INDEX_HTML_TEST_CONTENT = "<html>test</html>";

  @GetMapping(value = "/api/test")
  public String getHelloWorld() {
    return "Hello World";
  }

  @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
  public String getIndexHtmlTestContent() {
    return INDEX_HTML_TEST_CONTENT;
  }
}
