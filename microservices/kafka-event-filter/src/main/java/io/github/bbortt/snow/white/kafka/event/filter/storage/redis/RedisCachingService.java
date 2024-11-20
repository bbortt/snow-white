package io.github.bbortt.snow.white.kafka.event.filter.storage.redis;

import static java.lang.String.format;

import io.github.bbortt.snow.white.kafka.event.filter.service.CachingService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCachingService implements CachingService {

  private final RedisTemplate<String, Object> redisTemplate;

  public RedisCachingService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean apiExists(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return redisTemplate.hasKey(
      generateId(otelServiceName, apiName, apiVersion)
    );
  }

  private static String generateId(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return format("%s:%s:%s", otelServiceName, apiName, apiVersion);
  }
}
