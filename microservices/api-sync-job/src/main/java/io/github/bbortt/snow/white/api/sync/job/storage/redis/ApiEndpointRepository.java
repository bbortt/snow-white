package io.github.bbortt.snow.white.api.sync.job.storage.redis;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ApiEndpointRepository
  extends CrudRepository<ApiEndpointEntry, String> {
  List<ApiEndpointEntry> findByOtelServiceName(String otelServiceName);
  ApiEndpointEntry findByOtelServiceNameAndApiName(
    String otelServiceName,
    String apiName
  );
}
