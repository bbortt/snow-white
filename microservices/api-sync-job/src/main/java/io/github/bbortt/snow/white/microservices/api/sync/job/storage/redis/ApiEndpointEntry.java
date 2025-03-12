package io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Setter
@RedisHash("api_endpoints")
public class ApiEndpointEntry {

  @Id
  private String id; // Will be constructed as "{otelServiceName}:{apiName}:{apiVersion}"

  @Indexed
  private String otelServiceName;

  @Indexed
  private String apiName;

  @Indexed
  private String apiVersion;

  private String sourceUrl;

  public ApiEndpointEntry(
    String otelServiceName,
    String apiName,
    String apiVersion,
    String sourceUrl
  ) {
    this.id = generateId(otelServiceName, apiName, apiVersion);
    this.otelServiceName = otelServiceName;
    this.apiName = apiName;
    this.apiVersion = apiVersion;
    this.sourceUrl = sourceUrl;
  }

  private static String generateId(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return format("%s:%s:%s", otelServiceName, apiName, apiVersion);
  }

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
