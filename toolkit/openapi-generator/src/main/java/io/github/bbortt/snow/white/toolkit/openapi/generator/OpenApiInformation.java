package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static org.apache.commons.lang3.StringUtils.isBlank;

record OpenApiInformation(
  String apiName,
  String apiVersion,
  String serviceName
) {
  boolean isIncomplete() {
    return isBlank(apiName) || isBlank(apiVersion) || isBlank(serviceName);
  }
}
