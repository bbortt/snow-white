/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.redis;

import static io.github.bbortt.snow.white.commons.redis.RedisHashUtils.generateRedisApiInformationId;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PACKAGE;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Setter
@With(PACKAGE)
@RedisHash("api_endpoints")
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointEntry {

  @Id
  private @NonNull String id;

  @Indexed
  private @NonNull String otelServiceName;

  @Indexed
  private @NonNull String apiName;

  @Indexed
  private @NonNull String apiVersion;

  private @NonNull String sourceUrl;

  private @NonNull Short apiType;

  public ApiEndpointEntry(
    @NonNull String otelServiceName,
    @NonNull String apiName,
    @NonNull String apiVersion,
    @NonNull String sourceUrl,
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
      nonNull(id) &&
      id.equals(apiEndpointEntry.id)
    );
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
