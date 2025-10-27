/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class InfoEndpointConfigTest {

  @InjectMocks
  private InfoEndpointConfig infoEndpointConfig;

  @Mock
  private Environment environmentMock;

  @Test
  void shouldContributeActiveProfilesToInfoBuilder() {
    String[] activeProfiles = { "dev", "local" };
    when(environmentMock.getActiveProfiles()).thenReturn(activeProfiles);

    Info.Builder builder = new Info.Builder();
    infoEndpointConfig.contribute(builder);

    Info info = builder.build();
    assertEquals(activeProfiles, info.getDetails().get("activeProfiles"));
  }
}
