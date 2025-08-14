/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.api.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.mapper.QualityGateConfigMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
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

  public QualityGateConfig findQualityGateConfigByName(
    String qualityGateConfigName
  ) throws QualityGateNotFoundException {
    return queryQualityGateConfigByName(qualityGateConfigName).orElseThrow(() ->
      new QualityGateNotFoundException(qualityGateConfigName)
    );
  }

  private Optional<QualityGateConfig> queryQualityGateConfigByName(
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
