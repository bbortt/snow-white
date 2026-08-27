/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.mapper;

import static io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.index.AbstractApiIndexApiIT;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class ApiReferenceMapperIT extends AbstractApiIndexApiIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isRegisteredWithinSpringComponentModel() {
    assertThat(
      applicationContext.getBean(ApiReferenceMapper.class)
    ).isNotNull();
  }

  @Nested
  class FromDtoTest {

    @Autowired
    private ApiReferenceMapper apiReferenceMapper;

    @Test
    void shouldHaveDefaultValue_forIndexedAt() {
      var result = apiReferenceMapper.fromDto(
        GetAllApis200ResponseInner.builder()
          .serviceName("serviceName")
          .apiName("apiName")
          .apiVersion("apiVersion")
          .sourceUrl("sourceUrl")
          .apiType(OPENAPI)
          .build()
      );

      assertThat(result.getIndexedAt()).isNotNull();
    }
  }
}
