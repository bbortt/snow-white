package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExportTraceServiceRequestSerdesTest {

  @Nested
  class JsonSerde {

    private final TestData testData = TestData.builder().build();

    @Test
    void serializationAndDeserializationLoop() {
      var originalMessage = ExportTraceServiceRequest.newBuilder()
        .addResourceSpans(testData.resourceSpansWithAttributesOnEachLevel())
        .build();

      byte[] serializedData = ExportTraceServiceRequestSerdes.JsonSerde()
        .serializer()
        .serialize("test-topic", originalMessage);
      assertThat(serializedData).isNotNull();

      ExportTraceServiceRequest deserializedMessage =
        ExportTraceServiceRequestSerdes.JsonSerde()
          .deserializer()
          .deserialize("test-topic", serializedData);
      assertThat(deserializedMessage).isNotNull().isEqualTo(originalMessage);
    }
  }
}
