package io.github.bbortt.snow.white.commons.event.dto;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;

public interface TypedApiInformation {
  String getServiceName();
  String getApiName();
  String getApiVersion();
  ApiType getApiType();
}
