package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.AttributeFilterOperator.STRING_EQUALS;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AttributeFilter {

  private static final List<AttributeFilterOperator> STRING_OPERANDS = List.of(
    STRING_EQUALS
  );

  private final String key;
  private final AttributeFilterOperator operator;
  private final String value;

  public String toFluxString() {
    return (
      "  |> filter(fn: (r) => json.parse(v: r._value)[\"" +
      getKey() +
      "\"] " +
      getOperator() +
      " " +
      getValue() +
      ") "
    );
  }

  private String getKey() {
    return "[\"" + key + "\"]";
  }

  private String getOperator() {
    return operator.toFluxString();
  }

  private String getValue() {
    if (STRING_OPERANDS.contains(operator)) {
      return "\"" + value + "\"";
    }

    return value;
  }

  public static Builder attributeFilters() {
    return new Builder();
  }

  public static class Builder {

    private final List<AttributeFilter> attributeFilters = new ArrayList<>();

    public Builder with(AttributeFilter attributeFilter) {
      attributeFilters.add(attributeFilter);
      return this;
    }

    public List<AttributeFilter> build() {
      return attributeFilters;
    }
  }
}
