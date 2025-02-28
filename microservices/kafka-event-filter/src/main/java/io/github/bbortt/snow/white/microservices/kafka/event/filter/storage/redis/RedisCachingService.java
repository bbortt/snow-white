package io.github.bbortt.snow.white.microservices.kafka.event.filter.storage.redis;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisCachingService implements CachingService {

  @VisibleForTesting
  static final String HASH_PREFIX = "api_endpoints:";

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
    var id = HASH_PREFIX + generateId(otelServiceName, apiName, apiVersion);

    logger.debug("Checking if ID exists: {}", id);

    var result = redisTemplate.hasKey(id);

    logger.trace("ID '{}' {}", id, result ? "exists" : "does not exist");

    return result;
  }

  private static String generateId(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return format("%s:%s:%s", otelServiceName, apiName, apiVersion);
  }
}
