package io.github.bbortt.snow.white.microservices.kafka.event.filter.service;

public interface CachingService {
  boolean apiExists(String otelServiceName, String apiName, String apiVersion);
}
