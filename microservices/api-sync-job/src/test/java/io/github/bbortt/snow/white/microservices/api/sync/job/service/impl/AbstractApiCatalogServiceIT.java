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
import java.time.Duration;

abstract class AbstractApiCatalogServiceIT {

  ApiInformation fetchApiIndexAndAssertBasicInformation(
    ApiCatalogService fixture
  ) {
    var apiIndex = fixture.fetchApiIndex();

    assertThat(apiIndex).isCompletedWithValueMatchingWithin(
      index -> index.size() == 1,
      Duration.ofSeconds(1)
    );

    var apiInformation = apiIndex.join().iterator().next();
    assertThat(apiInformation).satisfies(
      info ->
        assertThat(info.getTitle()).isEqualTo("Swagger Petstore - OpenAPI 3.1"),
      info -> assertThat(info.getVersion()).isEqualTo("1.2.3"),
      info ->
        assertThat(info.getName()).isEqualTo("Swagger Petstore - OpenAPI 3.1"),
      info ->
        assertThat(info.getServiceName()).isEqualTo("example-application"),
      info -> assertThat(info.getApiType()).isEqualTo(OPENAPI),
      info -> assertThat(info.getLoadStatus()).isEqualTo(UNLOADED)
    );

    return apiInformation;
  }
}
