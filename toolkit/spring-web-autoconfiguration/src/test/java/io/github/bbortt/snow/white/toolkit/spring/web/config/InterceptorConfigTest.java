package io.github.bbortt.snow.white.toolkit.spring.web.config;

import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@ExtendWith({ MockitoExtension.class })
class InterceptorConfigTest {

  @Mock
  private OpenApiInformationEnhancer openApiInformationEnhancerMock;

  private InterceptorConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InterceptorConfig(openApiInformationEnhancerMock);
  }

  @Nested
  class AddInterceptors {

    @Mock
    private InterceptorRegistry registryMock;

    @Test
    void bindsSnowWhiteInformationEnhancerToRegistry() {
      fixture.addInterceptors(registryMock);

      verify(registryMock).addInterceptor(openApiInformationEnhancerMock);
    }
  }
}
