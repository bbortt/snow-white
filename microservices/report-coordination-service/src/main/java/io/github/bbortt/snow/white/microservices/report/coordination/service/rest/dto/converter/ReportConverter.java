package io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.converter;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class ReportConverter
  implements Converter<Report, ReportParameters> {

  @Override
  public ReportParameters convert(Report source) {
    return null;
  }
}
