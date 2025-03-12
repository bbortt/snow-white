package io.github.bbortt.snow.white.microservices.openapi.coverage.service.redis;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor
@RedisHash("api_endpoints")
public class ApiEndpointEntry {

  @Id
  private String id;

  @Indexed
  private String otelServiceName;

  @Indexed
  private String apiName;

  @Indexed
  private String apiVersion;

  private String sourceUrl;

  @Override
  public boolean equals(Object o) {
    return (
      o instanceof ApiEndpointEntry apiEndpointEntry &&
      nonNull(id) &&
      id.equals(apiEndpointEntry.id)
    );
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
