package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterionResult;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OpenApiCriterionResultMapperTest {

  @Nested
  class OpenApiCriteriaToIncludedInReport {

    @Test
    void shouldAlwaysReturnTrue() {
      assertThat(
        new OpenApiCriterionResultMapperImpl()
          .openApiCriteriaToIncludedInReport(null)
      )
        .isNotNull()
        .isTrue();
    }
  }

  @Nested
  class OpenApiCriteriaToName {

    @ParameterizedTest
    @EnumSource(OpenApiCriteria.class)
    void shouldExtractName(OpenApiCriteria openApiCriteria) {
      assertThat(
        new OpenApiCriterionResultMapperImpl()
          .openApiCriteriaToName(openApiCriteria)
      )
        .isNotNull()
        .isEqualTo(openApiCriteria.name());
    }
  }

  private static class OpenApiCriterionResultMapperImpl
    implements OpenApiCriterionResultMapper {

    @Override
    public Set<OpenApiCriterionResult> map(
      Set<
        io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult
      > openApiCriterionResults
    ) {
      throw new NotImplementedException();
    }

    @Override
    public OpenApiCriterionResult map(
      io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult source
    ) {
      throw new NotImplementedException();
    }
  }
}
