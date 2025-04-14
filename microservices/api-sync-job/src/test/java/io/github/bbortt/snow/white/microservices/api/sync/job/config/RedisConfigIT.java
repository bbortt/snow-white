/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@IntegrationTest
class RedisConfigIT {

  @Autowired
  private RedisConfig redisConfig;

  @Autowired
  private RedisConnectionFactory redisConnectionFactory;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Test
  void redisTemplateIsBean() {
    assertThat(redisTemplate).isEqualTo(
      redisConfig.redisTemplate(redisConnectionFactory)
    );
  }
}
