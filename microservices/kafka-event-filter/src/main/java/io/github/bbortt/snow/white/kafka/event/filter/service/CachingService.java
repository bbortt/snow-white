package io.github.bbortt.snow.white.kafka.event.filter.service;

public interface CachingService {
  boolean apiExists(String otelServiceName, String apiName, String apiVersion);
}
