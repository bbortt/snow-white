package io.github.bbortt.snow.white.commons.event;

import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@Builder
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class QualityGateCalculationRequestEvent {

  @Nonnull
  private String serviceName;

  @Nonnull
  private String apiName;

  private @Nullable String apiVersion;

  @Nonnull
  private String lookbackWindow;
}
