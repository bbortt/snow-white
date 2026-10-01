/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import lombok.Getter;

/**
 * A {@code sort} query parameter a list read cannot honour.
 *
 * <p>The message is written for the caller, not the log: it names the properties the endpoint
 * accepts, so a request can be corrected from the response alone. Each service maps this to a
 * {@code 400}; left unmapped it would reach the same default {@code 500} that resolving an
 * unpublished property against the entity used to produce.
 */
@Getter
public class InvalidSortException extends RuntimeException {

  private final String sort;

  public InvalidSortException(String sort, String publishedProperties) {
    super(
      "Cannot sort by '%s'. Expected 'property,(asc|desc)' with property one of: %s.".formatted(
        sort,
        publishedProperties
      )
    );
    this.sort = sort;
  }
}
