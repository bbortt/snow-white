package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

public enum ApiLoadStatus {
  // Not yet loaded
  UNLOADED,
  // Successfully loaded
  LOADED,
  // Load failed
  LOAD_FAILED,
  // No source url provided
  NO_SOURCE,
}
