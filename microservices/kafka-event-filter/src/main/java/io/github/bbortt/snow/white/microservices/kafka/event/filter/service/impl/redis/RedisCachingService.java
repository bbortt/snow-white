/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.service.impl.redis;

import static io.github.bbortt.snow.white.commons.redis.RedisHashUtils.generateRedisApiInformationId;

import com.google.common.annotations.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.CachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCachingService implements CachingService {

  @VisibleForTesting
  static final String HASH_PREFIX = "api_endpoints:";

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public boolean apiExists(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    var id =
      HASH_PREFIX +
      generateRedisApiInformationId(otelServiceName, apiName, apiVersion);

    logger.debug("Checking if ID exists: {}", id);

    var result = redisTemplate.hasKey(id);

    logger.trace("ID '{}' {}", id, result ? "exists" : "does not exist");

    return result;
  }
}
