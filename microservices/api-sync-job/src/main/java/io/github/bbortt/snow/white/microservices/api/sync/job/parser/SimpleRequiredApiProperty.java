package io.github.bbortt.snow.white.microservices.api.sync.job.parser;

public record SimpleRequiredApiProperty(String propertyName)
  implements ApiProperty {
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean isRequired() {
    return true;
  }
}
