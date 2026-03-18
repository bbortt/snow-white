/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.HousekeepingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class HousekeepingResourceTest {

  @Mock
  private HousekeepingService housekeepingServiceMock;

  private HousekeepingResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new HousekeepingResource(housekeepingServiceMock);
  }

  @Nested
  class HousekeepingTest {

    @Test
    void invokesService() {
      ResponseEntity<Void> response = fixture.housekeeping();

      assertThat(response).isNotNull();
      verify(housekeepingServiceMock).runHousekeeping();
    }

    @Test
    void returnsAccepted() {
      ResponseEntity<Void> response = fixture.housekeeping();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
      assertThat(response.getBody()).isNull();
    }
  }
}
