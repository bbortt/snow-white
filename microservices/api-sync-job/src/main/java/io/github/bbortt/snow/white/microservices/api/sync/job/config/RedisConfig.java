package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(
  basePackages = "io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis"
)
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
    RedisConnectionFactory connectionFactory
  ) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    return redisTemplate;
  }
}
