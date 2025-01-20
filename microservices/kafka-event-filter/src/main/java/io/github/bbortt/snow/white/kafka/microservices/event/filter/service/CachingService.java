package io.github.bbortt.snow.white.kafka.microservices.event.filter.service;

public interface CachingService {
  boolean apiExists(String otelServiceName, String apiName, String apiVersion);
}
