package io.github.bbortt.snow.white.proto.trace.v1;

import io.github.bbortt.snow.white.service.SpanService;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc.TraceServiceImplBase;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TraceServiceImpl extends TraceServiceImplBase {

  private final SpanService spanService;

  public TraceServiceImpl(SpanService spanService) {
    this.spanService = spanService;
  }

  public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver) {
    spanService.persistAll(request.getResourceSpansList());
  }
}
