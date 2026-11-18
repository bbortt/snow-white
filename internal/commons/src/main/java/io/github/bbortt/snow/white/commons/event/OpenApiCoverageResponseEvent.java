/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import java.util.Set;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record OpenApiCoverageResponseEvent(
  ApiInformation apiInformation,
  @Nullable Set<OpenApiTestResult> openApiTestResults,
  @With @Nullable String errorMessage
) implements ApiCoverageResponseEvent {
  /**
   * The largest this event may be on the wire, in bytes.
   * <p>
   * Kafka's defaults put this at 1 MiB on both ends, and a coverage response passes that: the
   * findings ride the result they explain, so the event grows with how much telemetry the window
   * held rather than with the document's size. Past the limit the producer refuses its own record
   * before the broker ever sees it, the stream client shuts down, and the input offset is never
   * committed - so the restart re-reads the same request and fails the same way.
   * <p>
   * It lives here because two sides have to agree on it: {@code openapi-coverage-stream} sizes its
   * producer by it, and whichever service creates the response topic first has to admit what that
   * producer may send. An operator raising the producer limit past this value has to raise the
   * topic's {@code max.message.bytes} to match.
   * <p>
   * shortcut: still headroom rather than a derived bound.
   * {@link io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding#MAX_EVIDENCE} removed the
   * traffic-shaped growth that made this necessary, so what remains scales with the number of
   * findings - that is, with the document under test, which is predictable and reviewable in a way
   * someone else's traffic volume was not. Derive it, or drop it back to the default, once a large
   * real specification has been measured through the capped path.
   */
  public static final int MAX_SERIALIZED_BYTES = 10 * 1024 * 1024;

  public OpenApiCoverageResponseEvent(
    ApiInformation apiInformation,
    @NonNull Set<OpenApiTestResult> openApiTestResults
  ) {
    this(apiInformation, openApiTestResults, null);
  }

  public OpenApiCoverageResponseEvent(
    ApiInformation apiInformation,
    @NonNull String errorMessage
  ) {
    this(apiInformation, null, errorMessage);
  }

  @Override
  public ApiType getApiType() {
    return OPENAPI;
  }
}
