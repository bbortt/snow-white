package io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception;

import static java.lang.String.format;

public class QualityGateNotFoundException extends Exception {

  public QualityGateNotFoundException(String qualityGateConfigName) {
    super(
      format(
        "No Quality-Gate configuration with ID '%s' exists!",
        qualityGateConfigName
      )
    );
  }
}
