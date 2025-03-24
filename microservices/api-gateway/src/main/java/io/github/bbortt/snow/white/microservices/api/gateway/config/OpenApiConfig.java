package io.github.bbortt.snow.white.microservices.api.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
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
