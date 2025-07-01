/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith({ MockitoExtension.class })
class RedisConfigTest {

  @Mock
  private RedisConnectionFactory connectionFactoryMock;

  @Nested
  class RedisTemplate {

    @Test
    void isBean() {
      var runner = new ApplicationContextRunner()
        .withUserConfiguration(RedisConfig.class);

      runner
        .withBean(RedisConnectionFactory.class, () -> connectionFactoryMock)
        .run(context ->
          assertThat(context).hasSingleBean(
            org.springframework.data.redis.core.RedisTemplate.class
          )
        );
    }
  }
}
