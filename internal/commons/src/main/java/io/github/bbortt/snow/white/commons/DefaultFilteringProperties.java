/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DefaultFilteringProperties {

  @VisibleForTesting
  static final String DEFAULT_API_NAME_ATTRIBUTE_KEY = "api.name";

  @VisibleForTesting
  static final String DEFAULT_API_VERSION_ATTRIBUTE_KEY = "api.version";

  @VisibleForTesting
  static final String DEFAULT_SERVICE_NAME_ATTRIBUTE_KEY =
    SERVICE_NAME.getKey();

  private String apiNameAttributeKey = DEFAULT_API_NAME_ATTRIBUTE_KEY;
  private String apiVersionAttributeKey = DEFAULT_API_VERSION_ATTRIBUTE_KEY;
  private String serviceNameAttributeKey = DEFAULT_SERVICE_NAME_ATTRIBUTE_KEY;
}
