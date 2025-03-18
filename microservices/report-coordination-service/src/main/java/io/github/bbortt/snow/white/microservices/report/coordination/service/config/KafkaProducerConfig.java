package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaProducerConfig {

  @Bean
  public ProducerFactory<
    String,
    QualityGateCalculationRequestEvent
  > producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(
      VALUE_SERIALIZER_CLASS_CONFIG,
      new JsonDeserializer<>(QualityGateCalculationRequestEvent.class)
    );
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<
    String,
    QualityGateCalculationRequestEvent
  > kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
