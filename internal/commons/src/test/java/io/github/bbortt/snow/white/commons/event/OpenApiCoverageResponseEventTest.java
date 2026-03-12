/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import java.util.Set;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageResponseEventTest {

  @Mock
  private ApiInformation apiInformationMock;

  private OpenApiCoverageResponseEvent fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageResponseEvent(
      apiInformationMock,
      Set.of(mock(OpenApiTestResult.class))
    );
  }

  @Test
  void shouldBeApiCoverageResponseEvent() {
    assertThat(fixture).isInstanceOf(ApiCoverageResponseEvent.class);
  }

  @Nested
  class ConstructorTest {

    @Test
    void shouldConstructResponseEventWithResults() {
      assertThat(fixture).hasNoNullFieldsOrPropertiesExcept("exception");
    }

    @Test
    void shouldConstructResponseEventWithException() {
      var customOpenApiCoverageResponseEvent = new OpenApiCoverageResponseEvent(
        apiInformationMock,
        mock(Throwable.class)
      );

      assertThat(
        customOpenApiCoverageResponseEvent
      ).hasNoNullFieldsOrPropertiesExcept("openApiTestResults");
    }
  }

  @Nested
  class GetApiTypeTest {

    @Test
    void shouldReturnOpenApi() {
      assertThat(fixture.getApiType()).isEqualTo(OPENAPI);
    }
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(OpenApiCoverageResponseEvent.class).verify();
  }
}
