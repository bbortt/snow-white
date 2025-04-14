/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SnowWhiteAutoConfigurationIT {

  @Autowired
  private SnowWhiteAutoConfiguration snowWhiteAutoConfiguration;

  @Test
  void autoconfigurationIsEnabled() {
    assertThat(snowWhiteAutoConfiguration).isNotNull();
  }
}
