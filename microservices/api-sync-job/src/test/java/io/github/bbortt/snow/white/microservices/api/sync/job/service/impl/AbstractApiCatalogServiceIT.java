/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;

abstract class AbstractApiCatalogServiceIT {

  ApiInformation fetchApiIndexAndAssertBasicInformation(
    ApiCatalogService fixture
  ) {
    var apiIndex = fixture.fetchApiIndex();

    assertThat(apiIndex)
      .hasSize(1)
      .first()
      .satisfies(
        apiInformation ->
          assertThat(apiInformation.getTitle()).isEqualTo(
            "Swagger Petstore - OpenAPI 3.1"
          ),
        apiInformation ->
          assertThat(apiInformation.getVersion()).isEqualTo("1.2.3"),
        apiInformation ->
          assertThat(apiInformation.getName()).isEqualTo(
            "Swagger Petstore - OpenAPI 3.1"
          ),
        apiInformation ->
          assertThat(apiInformation.getServiceName()).isEqualTo(
            "example-application"
          ),
        apiInformation ->
          assertThat(apiInformation.getApiType()).isEqualTo(OPENAPI),
        apiInformation ->
          assertThat(apiInformation.getLoadStatus()).isEqualTo(UNLOADED)
      );

    return apiIndex.iterator().next();
  }
}
