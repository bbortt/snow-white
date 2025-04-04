package io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QualityGateNotFoundExceptionTest {

  @Test
  void shouldConstructMessage() {
    assertThat(new QualityGateNotFoundException("foo")).hasMessage(
      "No Quality-Gate configuration with ID 'foo' exists!"
    );
  }
}
