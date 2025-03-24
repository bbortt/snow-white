package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static java.util.Collections.singletonList;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

  private final QualityGateApiProperties qualityGateApiProperties;

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
      .info(new Info().title("Quality-Gate API"))
      .servers(
        singletonList(
          new Server().url(qualityGateApiProperties.getPublicApiGatewayUrl())
        )
      );
  }
}
