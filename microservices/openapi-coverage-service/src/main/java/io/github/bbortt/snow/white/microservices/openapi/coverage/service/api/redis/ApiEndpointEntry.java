/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Builder
@Getter
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
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
