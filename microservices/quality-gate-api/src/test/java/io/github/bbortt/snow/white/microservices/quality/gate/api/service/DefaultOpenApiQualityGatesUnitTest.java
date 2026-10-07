/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class DefaultOpenApiQualityGatesUnitTest {

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  @InjectMocks
  private DefaultOpenApiQualityGates fixture;

  @Nested
  class GetDefaultOpenApiCoverageConfigurationsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_011_FOUR_PREDEFINED_GATES_FIXED_COMPOSITION)
    void shouldContainFourQualityGateDefinitions() {
      doAnswer(invocation ->
        Optional.of(
          OpenApiCoverageConfiguration.builder()
            .name(invocation.getArgument(0))
            .build()
        )
      )
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());

      assertThat(fixture.getDefaultOpenApiCoverageConfigurations())
        .allSatisfy(qualityGateConfiguration ->
          assertThat(qualityGateConfiguration.getIsPredefined()).isTrue()
        )
        .satisfiesExactly(
          // basic-coverage
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(configuration.getOpenApiCoverageConfigurations())
                  .extracting(mapping ->
                    mapping.getOpenApiCoverageConfiguration().getName()
                  )
                  .containsExactlyInAnyOrder(
                    PATH_COVERAGE.name(),
                    HTTP_METHOD_COVERAGE.name(),
                    OPERATION_SUCCESS_COVERAGE.name(),
                    POSITIVE_RESPONSE_CODE_COVERAGE.name(),
                    REQUIRED_PARAMETER_COVERAGE.name(),
                    NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES.name()
                  ),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  80
                )
            ),
          // full-feature
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(configuration.getOpenApiCoverageConfigurations())
                  .extracting(mapping ->
                    mapping.getOpenApiCoverageConfiguration().getName()
                  )
                  .containsExactlyInAnyOrder(
                    HTTP_METHOD_COVERAGE.name(),
                    OPERATION_SUCCESS_COVERAGE.name(),
                    RESPONSE_CODE_COVERAGE.name(),
                    PARAMETER_COVERAGE.name(),
                    CONTENT_TYPE_COVERAGE.name(),
                    REQUIRED_ERROR_FIELDS_COVERAGE.name(),
                    NO_UNDOCUMENTED_RESPONSE_CODES.name()
                  ),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  100
                )
            ),
          // minimal
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(
                  configuration.getOpenApiCoverageConfigurations()
                ).satisfiesExactly(openApiConfiguration ->
                  assertThat(
                    openApiConfiguration
                      .getOpenApiCoverageConfiguration()
                      .getName()
                  ).isEqualTo(PATH_COVERAGE.name())
                ),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  80
                )
            ),
          // dry-run
          qualityGateConfiguration ->
            assertThat(
              qualityGateConfiguration.getOpenApiCoverageConfigurations()
            ).isEmpty()
        );
    }

    /**
     * Containment is a declared fact about target spaces, not a composition rule: every gate that
     * requires a contained criterion goes on requiring it without its containers. {@code
     * basic-coverage} requires three such criteria, and naming all three containers it leaves out is
     * what makes a composition that started applying containment fail here - pulling in even one of
     * them drops that container from this set.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
    )
    void containmentAddsNoCriterionToAPredefinedGate() {
      doAnswer(invocation ->
        Optional.of(
          OpenApiCoverageConfiguration.builder()
            .name(invocation.getArgument(0))
            .build()
        )
      )
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());

      var criteria = criteriaOf(
        gateNamed(
          fixture.getDefaultOpenApiCoverageConfigurations(),
          "basic-coverage"
        )
      );

      var containersLeftOut = criteria
        .stream()
        .flatMap(criterion -> criterion.getContainingCriteria().stream())
        .filter(container -> !criteria.contains(container))
        .collect(toSet());

      assertThat(containersLeftOut).containsExactlyInAnyOrder(
        RESPONSE_CODE_COVERAGE,
        PARAMETER_COVERAGE,
        NO_UNDOCUMENTED_RESPONSE_CODES
      );
    }

    /**
     * The predefined gates are a ladder, and this is the rung that is expressible as a subset:
     * {@code minimal} is a reachability check, so every criterion it requires has to be required by
     * the pragmatic baseline above it. Dropping path coverage from {@code basic-coverage} once left
     * a gate named "minimal" demanding something the baseline never measured, and this fails if
     * that inversion returns.
     *
     * <p>The rung above cannot be written the same way: {@code full-feature} covers
     * {@code basic-coverage}'s response-code, parameter and no-undocumented criteria through
     * containment, but reaches path coverage only through the implication that full method coverage
     * is full path coverage — true at its 100% threshold, and not a subset relation.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_011_FOUR_PREDEFINED_GATES_FIXED_COMPOSITION)
    void minimalIsASubsetOfBasicCoverage() {
      doAnswer(invocation ->
        Optional.of(
          OpenApiCoverageConfiguration.builder()
            .name(invocation.getArgument(0))
            .build()
        )
      )
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());

      var gates = fixture.getDefaultOpenApiCoverageConfigurations();

      assertThat(criteriaOf(gateNamed(gates, "minimal")))
        .isNotEmpty()
        .isSubsetOf(criteriaOf(gateNamed(gates, "basic-coverage")));
    }

    private QualityGateConfiguration gateNamed(
      Set<QualityGateConfiguration> gates,
      String name
    ) {
      return gates
        .stream()
        .filter(qualityGateConfiguration ->
          name.equals(qualityGateConfiguration.getName())
        )
        .findFirst()
        .orElseThrow();
    }

    private Set<OpenApiCoverageCriteria> criteriaOf(
      QualityGateConfiguration qualityGateConfiguration
    ) {
      return qualityGateConfiguration
        .getOpenApiCoverageConfigurations()
        .stream()
        .map(mapping ->
          OpenApiCoverageCriteria.valueOf(
            mapping.getOpenApiCoverageConfiguration().getName()
          )
        )
        .collect(toSet());
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
    void shouldResultInExceptionWhenOpenApiConfigurationsDoNotExist() {
      assertThatThrownBy(() ->
        fixture.getDefaultOpenApiCoverageConfigurations()
      ).isInstanceOf(IllegalStateException.class);

      verify(openApiCoverageConfigurationRepositoryMock).findByName(
        anyString()
      );
    }
  }
}
