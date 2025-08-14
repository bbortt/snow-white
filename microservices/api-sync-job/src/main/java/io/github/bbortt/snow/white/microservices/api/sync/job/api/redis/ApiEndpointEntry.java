/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.api.redis;

import static io.github.bbortt.snow.white.commons.redis.RedisHashUtils.generateRedisApiInformationId;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import jakarta.annotation.Nonnull;
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
  private @Nonnull String id; // Will be constructed as "{otelServiceName}:{apiName}:{apiVersion}"

  @Indexed
  private @Nonnull String otelServiceName;

  @Indexed
  private @Nonnull String apiName;

  @Indexed
  private @Nonnull String apiVersion;

  private @Nonnull String sourceUrl;

  private @Nonnull Integer apiType;

  public ApiEndpointEntry(
    @Nonnull String otelServiceName,
    @Nonnull String apiName,
    @Nonnull String apiVersion,
    @Nonnull String sourceUrl,
    ApiType apiType
  ) {
    this.id = generateRedisApiInformationId(
      otelServiceName,
      apiName,
      apiVersion
    );
    this.otelServiceName = otelServiceName;
    this.apiName = apiName;
    this.apiVersion = apiVersion;
    this.sourceUrl = sourceUrl;
    this.apiType = apiType.getVal();
  }

  @Override
  public boolean equals(Object o) {
    return (
      o instanceof ApiEndpointEntry apiEndpointEntry &&
      id.equals(apiEndpointEntry.id)
    );
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
