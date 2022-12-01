package io.github.bbortt.snow.white.proto.trace.v1;

import io.github.bbortt.snow.white.service.SpanService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TraceServiceImpl implements TraceService {

  private final SpanService spanService;

  public TraceServiceImpl(SpanService spanService) {
    this.spanService = spanService;
  }

  @Override
  public Uni<ExportTraceServiceResponse> export(ExportTraceServiceRequest request) {
    return spanService
      .persistAll(request.getResourceSpansList())
      .replaceWith(Uni.createFrom().item(ExportTraceServiceResponse.newBuilder().getDefaultInstanceForType()));
  }
}
