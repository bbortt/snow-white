package io.github.bbortt.snow.white.commons.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@With
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class QualityGateCalculationRequestEvent {

  private final UUID calculationId;

  @Nonnull
  private final String serviceName;

  @Nonnull
  private final String apiName;

  private @Nullable String apiVersion;

  @Nonnull
  private final String lookbackWindow;
}
