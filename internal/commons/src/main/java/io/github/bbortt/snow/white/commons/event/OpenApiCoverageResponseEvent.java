package io.github.bbortt.snow.white.commons.event;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriteriaResult;
import java.util.Set;

public record OpenApiCoverageResponseEvent(
  Set<OpenApiCriteriaResult> openApiCriteria
) {}
