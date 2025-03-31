package io.github.bbortt.snow.white.microservices.api.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

  private final ApiGatewayProperties apiGatewayProperties;
  //    @Bean
  //    public OpenAPI openApi(){
  //        return new OpenAPI()
  //                .servers(List.of(
  //                        new Server()
  //                                .url(getUrl(apiGatewayProperties.getQualityGateApiUrl())),
  //                        new Server().url(getUrl(apiGatewayProperties.getReportCoordinationServiceUrl()))
  //                ))
  //                .info(new Info().title("Snow-White")
  //                        //.version(serviceVersion)
  //                );
  //    }

}
