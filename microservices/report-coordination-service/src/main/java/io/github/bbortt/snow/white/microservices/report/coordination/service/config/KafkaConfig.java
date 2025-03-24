package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConfig {

  private static final Map<String, Object> KAFKA_PROPS = Map.of(
    KEY_DESERIALIZER_CLASS_CONFIG,
    StringSerializer.class,
    VALUE_DESERIALIZER_CLASS_CONFIG,
    new JsonDeserializer<>(QualityGateCalculationRequestEvent.class),
    KEY_SERIALIZER_CLASS_CONFIG,
    StringSerializer.class,
    VALUE_SERIALIZER_CLASS_CONFIG,
    new JsonDeserializer<>(QualityGateCalculationRequestEvent.class)
  );

  @Bean
  public ConsumerFactory<
    String,
    QualityGateCalculationRequestEvent
  > consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(KAFKA_PROPS);
  }

  @Bean
  public ProducerFactory<
    String,
    QualityGateCalculationRequestEvent
  > producerFactory() {
    return new DefaultKafkaProducerFactory<>(KAFKA_PROPS);
  }

  @Bean
  public KafkaTemplate<
    String,
    QualityGateCalculationRequestEvent
  > kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
