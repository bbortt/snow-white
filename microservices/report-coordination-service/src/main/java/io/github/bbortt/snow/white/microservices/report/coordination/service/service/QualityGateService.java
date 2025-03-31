package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.api.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.mapper.QualityGateConfigMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class QualityGateService {

  private final QualityGateApi qualityGateApi;
  private final QualityGateConfigMapper qualityGateConfigMapper;

  public Optional<QualityGateConfig> findQualityGateConfigByName(
    String qualityGateConfigName
  ) {
    try {
      return Optional.ofNullable(
        qualityGateApi.getQualityGateByName(qualityGateConfigName)
      ).map(qualityGateConfigMapper::fromDto);
    } catch (RestClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        return Optional.empty();
      }

      throw e;
    }
  }
}
