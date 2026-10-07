package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class JUnitJupiterApiExtensionUnitTest {

  @Test
  void registersSnowWhiteOpenTelemetryExtensionAutomatically()
    throws IOException {
    try (
      var serviceFile = requireNonNull(
        getClass()
          .getClassLoader()
          .getResourceAsStream(
            "META-INF/services/org.junit.jupiter.api.extension.Extension"
          )
      )
    ) {
      var registeredExtension = new String(
        serviceFile.readAllBytes(),
        UTF_8
      ).trim();

      assertThat(registeredExtension).isEqualTo(
        SnowWhiteOpenTelemetryExtension.class.getName()
      );
    }
  }
}
