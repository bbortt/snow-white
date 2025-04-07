package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.CriteriaApi;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.OpenApiCoverageConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CriteriaResource implements CriteriaApi {

  private final OpenApiCoverageConfigurationMapper openApiCoverageConfigurationMapper;
  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;

  @Override
  public ResponseEntity<List<OpenApiCriterion>> listOpenApiCriteria() {
    var openApiCoverageConfigurations =
      openApiCoverageConfigurationService.getAllOpenapiCoverageConfigurations();

    return ResponseEntity.ok(
      openApiCoverageConfigurationMapper.toDtos(openApiCoverageConfigurations)
    );
  }
}
