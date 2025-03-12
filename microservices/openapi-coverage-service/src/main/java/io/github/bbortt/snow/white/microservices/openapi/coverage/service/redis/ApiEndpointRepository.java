package io.github.bbortt.snow.white.microservices.openapi.coverage.service.redis;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ApiEndpointRepository
  extends CrudRepository<ApiEndpointEntry, String> {
  Optional<
    ApiEndpointEntry
  > findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
    @Param("otelServiceName") String otelServiceName,
    @Param("apiName") String apiName,
    @Param("apiVersion") String apiVersion
  );
}
