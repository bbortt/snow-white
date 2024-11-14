package io.github.bbortt.snow.white.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.api.sync.job.IntegrationTest;
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
  void redisConnectionFactoryIsBean() {
    assertThat(redisConnectionFactory).isEqualTo(
      redisConfig.redisConnectionFactory()
    );
  }

  @Test
  void redisTemplateIsBean() {
    assertThat(redisTemplate).isEqualTo(
      redisConfig.redisTemplate(redisConnectionFactory)
    );
  }
}
