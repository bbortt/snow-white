package io.github.bbortt.snow.white.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryRegistryConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryRegistryConfiguration.class);

  /**
   * Unfortunately the OpenTelemetry Java instrumentation agent registers with Micrometer’s
   * {@link Metrics#globalRegistry}, while Spring uses its own registry instance via dependency
   * injection. If the OpenTelemetryMeterRegistry ends up in the wrong MeterRegistry instance, it
   * is not used by Spring.
   */
  @Bean
  @ConditionalOnBean(OpenTelemetryAgentCondition.class)
  public MeterRegistry meterRegistry() {
    logger.debug("Configuring micrometer registry for Open-Telemetry agent");

    Optional<MeterRegistry> otelRegistry = Metrics.globalRegistry
      .getRegistries()
      .stream()
      .filter(registry -> registry.getClass().getName().contains("OpenTelemetryMeterRegistry"))
      .findAny();
    otelRegistry.ifPresent(Metrics.globalRegistry::remove);

    return otelRegistry.orElse(null);
  }

  @Configuration
  @ConditionalOnClass(name = "io.opentelemetry.javaagent.OpenTelemetryAgent")
  public class OpenTelemetryAgentCondition {}
}
