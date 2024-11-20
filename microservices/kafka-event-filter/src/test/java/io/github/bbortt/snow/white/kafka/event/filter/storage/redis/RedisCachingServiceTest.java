package io.github.bbortt.snow.white.kafka.event.filter.storage.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith({ MockitoExtension.class })
class RedisCachingServiceTest {

  @Mock
  private RedisTemplate<String, Object> redisTemplateMock;

  private RedisCachingService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RedisCachingService(redisTemplateMock);
  }

  @Nested
  class ApiExists {

    static Stream<Boolean> usesRedisTemplateToCheckKeys() {
      return Stream.of(true, false);
    }

    @MethodSource
    @ParameterizedTest
    void usesRedisTemplateToCheckKeys(boolean apiExists) {
      var otelServiceName = "otelServiceName";
      var apiName = "apiName";
      var apiVersion = "apiVersion";

      doReturn(apiExists)
        .when(redisTemplateMock)
        .hasKey(otelServiceName + ":" + apiName + ":" + apiVersion);

      var result = fixture.apiExists(otelServiceName, apiName, apiVersion);

      assertThat(result).isEqualTo(apiExists);
    }
  }
}
