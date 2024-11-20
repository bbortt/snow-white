package io.github.bbortt.snow.white.kafka.event.filter.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.kafka.event.filter.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@IntegrationTest
@SpringBootTest(
  properties = {
    "io.github.bbortt.snow.white.kafka.event.filter.schema-registry-url=mock://RedisConfigIT",
  }
)
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
