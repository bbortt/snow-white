/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper.ApiTestResultMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class ApiTestResultMapperIT extends AbstractReportCoordinationServiceIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isRegisteredWithinSpringComponentModel() {
    assertThat(
      applicationContext.getBean(ApiTestResultMapper.class)
    ).isNotNull();
  }
}
