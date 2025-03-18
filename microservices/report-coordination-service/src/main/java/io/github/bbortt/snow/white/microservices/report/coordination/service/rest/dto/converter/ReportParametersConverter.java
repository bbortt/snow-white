package io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.converter;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import org.springframework.core.convert.converter.Converter;

public final class ReportParametersConverter
  implements Converter<ReportParameters, Report> {

  @Override
  public Report convert(ReportParameters source) {
    return null;
  }
}
