/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import org.junit.jupiter.api.Test;

class AnnotationTest {

  @Test
  @SnowWhiteInformation(
    apiName = "foo",
    apiVersion = "1.0.0",
    serviceName = "bar"
  )
  void annotationIsValid() throws NoSuchMethodException {
    assertThat(
      AnnotationTest.class.getDeclaredMethod(
        "annotationIsValid"
      ).getAnnotations()
    ).hasSize(2);
  }
}
