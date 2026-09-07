/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class TempoConfiguredConditionUnitTest {

  private final TempoConfiguredCondition fixture =
    new TempoConfiguredCondition();

  private final MockEnvironment mockEnvironment = new MockEnvironment();

  @Mock
  private ConditionContext conditionContextMock;

  @Mock
  private AnnotatedTypeMetadata annotatedTypeMetadataMock;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(mockEnvironment).when(conditionContextMock).getEnvironment();
  }

  @Nested
  class MatchesTest {

    @Test
    void shouldReturnFalse_withoutUrl() {
      mockEnvironment.setProperty("tempo.token", "token");
      mockEnvironment.setProperty("tempo.username", "username");
      mockEnvironment.setProperty("tempo.password", "password");

      assertThat(
        fixture.matches(conditionContextMock, annotatedTypeMetadataMock)
      ).isFalse();
    }

    @Test
    void shouldReturnFalse_withoutTokenNorUsername() {
      mockEnvironment.setProperty("tempo.url", "url");

      mockEnvironment.setProperty("tempo.password", "password");

      assertThat(
        fixture.matches(conditionContextMock, annotatedTypeMetadataMock)
      ).isFalse();
    }

    @Test
    void shouldReturnFalse_withoutTokenNorPassword() {
      mockEnvironment.setProperty("tempo.url", "url");

      mockEnvironment.setProperty("tempo.username", "username");

      assertThat(
        fixture.matches(conditionContextMock, annotatedTypeMetadataMock)
      ).isFalse();
    }

    @Test
    void shouldReturnTrue_withUrlAndToken() {
      mockEnvironment.setProperty("tempo.url", "url");

      mockEnvironment.setProperty("tempo.token", "token");

      assertThat(
        fixture.matches(conditionContextMock, annotatedTypeMetadataMock)
      ).isTrue();
    }

    @Test
    void shouldReturnTrue_withUrlAndUsernameAndPassword() {
      mockEnvironment.setProperty("tempo.url", "url");

      mockEnvironment.setProperty("tempo.username", "username");
      mockEnvironment.setProperty("tempo.password", "password");

      assertThat(
        fixture.matches(conditionContextMock, annotatedTypeMetadataMock)
      ).isTrue();
    }
  }
}
