/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasLength;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import org.springframework.stereotype.Service;

@Service
public class OpenApiValidationService {

  public ApiInformation validateApiInformationFromIndex(
    ApiInformation apiInformation,
    ParsingMode parsingMode
  ) {
    if (!hasLength(apiInformation.getSourceUrl())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without source URL: %s!",
            apiInformation.getTitle()
          )
        );
      } else {
        return apiInformation.withLoadStatus(NO_SOURCE);
      }
    }

    if (isNull(apiInformation.getApiType())) {
      if (STRICT.equals(parsingMode)) {
        throw new IllegalArgumentException(
          format(
            "Encountered API in index without type definition: %s!",
            apiInformation.getTitle()
          )
        );
      } else {
        return apiInformation.withLoadStatus(LOAD_FAILED);
      }
    }

    return apiInformation.withLoadStatus(LOADED);
  }
}
