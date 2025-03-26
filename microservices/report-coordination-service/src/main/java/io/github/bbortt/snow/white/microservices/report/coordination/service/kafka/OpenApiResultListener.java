package io.github.bbortt.snow.white.microservices.report.coordination.service.kafka;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.DEFAULT_CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC;
import static java.lang.String.format;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.openapi.OpenApiReportCalculator;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenApiResultListener {

  private final QualityGateService qualityGateService;
  private final ReportService reportService;

  @KafkaListener(
    groupId = "${" + CONSUMER_GROUP_ID + ":" + DEFAULT_CONSUMER_GROUP_ID + "}",
    topics = { "${" + OPENAPI_CALCULATION_RESPONSE_TOPIC + "}" }
  )
  public void persistOpenApiCoverageResponseIfReportIsPresent(
    @Header(name = RECEIVED_KEY) UUID key,
    @Payload OpenApiCoverageResponseEvent openApiCoverageResponseEvent
  ) {
    reportService
      .findReportByCalculationId(key)
      .map(report ->
        new QualityGateConfigurationParameters(
          report,
          qualityGateService
            .findQualityGateConfigByName(report.getQualityGateConfigName())
            .orElseThrow(() ->
              new IllegalStateException(
                format(
                  "Unreachable state, Quality-Gate configuration '%s' must exist at this point!",
                  report.getQualityGateConfigName()
                )
              )
            )
        )
      )
      .ifPresent(configurationParameters ->
        calculateAndPersistQualityGateReport(
          configurationParameters,
          openApiCoverageResponseEvent
        )
      );
  }

  private void calculateAndPersistQualityGateReport(
    QualityGateConfigurationParameters configurationParameters,
    OpenApiCoverageResponseEvent openApiCoverageResponseEvent
  ) {
    reportService.update(
      configurationParameters
        .report()
        .withOpenApiCoverage(
          new OpenApiReportCalculator(
            openApiCoverageResponseEvent,
            configurationParameters
              .qualityGateConfig()
              .getOpenApiCoverageConfig()
          ).calculateReport()
        )
        .withUpdatedReportStatus()
    );
  }

  private record QualityGateConfigurationParameters(
    Report report,
    QualityGateConfig qualityGateConfig
  ) {}
}
