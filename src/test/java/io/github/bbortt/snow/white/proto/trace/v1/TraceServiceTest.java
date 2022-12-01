package io.github.bbortt.snow.white.proto.trace.v1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceService;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TraceServiceTest {

  @GrpcClient
  TraceService traceService;

  @Test
  void exportPersistsAllSpans() {
    ExportTraceServiceResponse response = traceService
      .export(ExportTraceServiceRequest.newBuilder().build())
      .await()
      .atMost(Duration.ofSeconds(5));
    assertFalse(response.hasPartialSuccess());
  }
}
