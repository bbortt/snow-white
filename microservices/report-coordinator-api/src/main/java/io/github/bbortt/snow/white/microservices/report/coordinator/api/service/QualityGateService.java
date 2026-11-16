/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.api.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.mapper.QualityGateConfigMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class QualityGateService {

  private QualityGateApi qualityGateApi;
  private QualityGateConfigMapper qualityGateConfigMapper;

  @Autowired
  public void setQualityGateApi(QualityGateApi qualityGateApi) {
    this.qualityGateApi = qualityGateApi;
  }

  @Autowired
  public void setQualityGateConfigMapper(
    QualityGateConfigMapper qualityGateConfigMapper
  ) {
    this.qualityGateConfigMapper = qualityGateConfigMapper;
  }

  @WithSpan
  @Retryable(
    retryFor = {
      HttpServerErrorException.class,
      ResourceAccessException.class,
    },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public QualityGateConfig findQualityGateConfigByName(
    String qualityGateConfigName
  ) throws QualityGateNotFoundException {
    try {
      return Optional.ofNullable(
        qualityGateApi.getQualityGateByName(qualityGateConfigName)
      )
        .map(qualityGateConfigMapper::fromDto)
        .orElseThrow(() ->
          QualityGateNotFoundException.forGateName(qualityGateConfigName)
        );
    } catch (RestClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        throw toQualityGateNotFoundException(qualityGateConfigName, e);
      }

      throw e;
    }
  }

  /**
   * Surfaces the rejection the Quality-Gate API wrote, rather than composing one here: the gates
   * are that service's to own, and it is the only side that can see whether its gate file defines
   * no gates at all — a name that could not have existed, as opposed to one merely misspelt. The
   * status stays what it was, and no second call is made.
   *
   * <p>Falls back to this service's own wording when the rejection carried no body to surface,
   * which is also what a {@code 404} from anything but the Quality-Gate API itself earns.
   */
  @RealizesSw(SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE)
  private static QualityGateNotFoundException toQualityGateNotFoundException(
    String qualityGateConfigName,
    RestClientResponseException e
  ) {
    var ownerMessage = ownerMessageOf(e);

    return hasText(ownerMessage)
      ? QualityGateNotFoundException.surfacing(ownerMessage)
      : QualityGateNotFoundException.forGateName(qualityGateConfigName);
  }

  private static @Nullable String ownerMessageOf(
    RestClientResponseException e
  ) {
    try {
      var error = e.getResponseBodyAs(Error.class);
      return isNull(error) ? null : error.getMessage();
    } catch (RestClientException unparseable) {
      // a 404 from anything but this API — a gateway, a proxy — carries a body we cannot read
      return null;
    }
  }
}
