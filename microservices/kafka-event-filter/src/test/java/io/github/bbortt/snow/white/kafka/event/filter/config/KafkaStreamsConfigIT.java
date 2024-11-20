package io.github.bbortt.snow.white.kafka.event.filter.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.kafka.event.filter.IntegrationTest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serde;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@IntegrationTest
@SpringBootTest(
  properties = {
    "io.github.bbortt.snow.white.kafka.event.filter.schema-registry-url=mock://KafkaStreamsConfigIT",
  }
)
class KafkaStreamsConfigIT {

  @Autowired
  private KafkaEventFilterProperties kafkaEventFilterProperties;

  @Autowired
  private Properties snowWhiteKafkaProperties;

  @Autowired
  private Serde<ExportTraceServiceRequest> exportTraceServiceRequestSerde;

  @Autowired
  private KafkaStreamsConfig kafkaStreamsConfig;

  @Test
  void snowWhiteKafkaPropertiesIsBean() {
    assertThat(snowWhiteKafkaProperties).isEqualTo(
      kafkaStreamsConfig.snowWhiteKafkaProperties(kafkaEventFilterProperties)
    );
  }

  @Test
  void exportTraceServiceRequestSerdeIsBean() {
    assertThat(exportTraceServiceRequestSerde).isEqualTo(
      kafkaStreamsConfig.exportTraceServiceRequestSerde(
        snowWhiteKafkaProperties
      )
    );
  }
}
