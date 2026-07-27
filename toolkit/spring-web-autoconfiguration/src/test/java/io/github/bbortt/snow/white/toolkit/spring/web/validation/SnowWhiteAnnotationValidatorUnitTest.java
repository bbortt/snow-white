/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@ExtendWith({ MockitoExtension.class })
class SnowWhiteAnnotationValidatorUnitTest {

  @Mock
  private ApplicationReadyEvent eventMock;

  @Mock
  private ConfigurableApplicationContext contextMock;

  private SnowWhiteAnnotationValidator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SnowWhiteAnnotationValidator();
    doReturn(contextMock).when(eventMock).getApplicationContext();
  }

  @Test
  void passesValidation_whenControllerIsFullyAnnotatedAtClassLevel() {
    doReturn(Map.of("controller", new CompleteClassAnnotatedController()))
      .when(contextMock)
      .getBeansWithAnnotation(Controller.class);

    assertThatNoException().isThrownBy(() ->
      fixture.validateOnStartup(eventMock)
    );
  }

  @Test
  void passesValidation_whenAnnotationsAreCompletedByMerge() {
    doReturn(Map.of("controller", new MergedAnnotationController()))
      .when(contextMock)
      .getBeansWithAnnotation(Controller.class);

    assertThatNoException().isThrownBy(() ->
      fixture.validateOnStartup(eventMock)
    );
  }

  @Test
  void passesValidation_whenNoAnnotationPresent() {
    doReturn(Map.of("controller", new UnannotatedController()))
      .when(contextMock)
      .getBeansWithAnnotation(Controller.class);

    assertThatNoException().isThrownBy(() ->
      fixture.validateOnStartup(eventMock)
    );
  }

  @Test
  void throwsIllegalStateException_whenAnnotationIsIncomplete() {
    doReturn(Map.of("controller", new IncompleteAnnotationController()))
      .when(contextMock)
      .getBeansWithAnnotation(Controller.class);

    assertThatThrownBy(() -> fixture.validateOnStartup(eventMock))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Incomplete @SnowWhiteInformation");
  }

  @SnowWhiteInformation(serviceName = "svc", apiName = "api", apiVersion = "v1")
  private static class CompleteClassAnnotatedController {

    @GetMapping("/test")
    public void endpoint() {
      throw new UnsupportedOperationException();
    }
  }

  @SnowWhiteInformation(apiName = "api", apiVersion = "v1")
  private static class MergedAnnotationController {

    @GetMapping("/test")
    @SnowWhiteInformation(serviceName = "svc", operationId = "doThing")
    public void endpoint() {
      throw new UnsupportedOperationException();
    }
  }

  private static class UnannotatedController {

    @GetMapping("/test")
    public void endpoint() {
      throw new UnsupportedOperationException();
    }
  }

  @SnowWhiteInformation(serviceName = "svc")
  private static class IncompleteAnnotationController {

    @GetMapping("/test")
    public void endpoint() {
      throw new UnsupportedOperationException();
    }
  }
}
