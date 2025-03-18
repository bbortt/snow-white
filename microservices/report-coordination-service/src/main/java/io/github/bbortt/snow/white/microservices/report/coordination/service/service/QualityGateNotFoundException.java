package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static java.lang.String.format;

public class QualityGateNotFoundException extends Throwable {

  public QualityGateNotFoundException(String qualityGateConfigName) {
    super(
      format(
        "No Quality-Gate configuration with ID '%s' exists!",
        qualityGateConfigName
      )
    );
  }
}
