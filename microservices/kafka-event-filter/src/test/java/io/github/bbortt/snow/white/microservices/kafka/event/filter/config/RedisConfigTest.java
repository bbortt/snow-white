/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@ExtendWith({ MockitoExtension.class })
class RedisConfigTest {

  @Mock
  private RedisConnectionFactory redisConnectionFactoryMock;

  private RedisConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RedisConfig();
  }

  @Test
  void connectionFactoryIsBeingSet() {
    RedisTemplate<String, Object> redisTemplate = fixture.redisTemplate(
      redisConnectionFactoryMock
    );

    assertThat(redisTemplate.getConnectionFactory()).isEqualTo(
      redisConnectionFactoryMock
    );
  }

  @Test
  void keySerializerIsStringRedisSerializer() {
    RedisTemplate<String, Object> redisTemplate = fixture.redisTemplate(
      redisConnectionFactoryMock
    );

    assertThat(redisTemplate.getKeySerializer()).isInstanceOf(
      StringRedisSerializer.class
    );
  }
}
