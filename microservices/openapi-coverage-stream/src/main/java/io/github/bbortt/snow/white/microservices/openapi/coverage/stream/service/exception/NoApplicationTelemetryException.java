/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;

/**
 * A calculation stopped because a test harness exported every span this window could attribute to a
 * documented operation, and the application under test none of them.
 * <p>
 * Asked of the attributed spans and not of the window as read, so the message names the three ways
 * it happens rather than asserting the first: an application that is not instrumented, one whose
 * telemetry never reached the backend, and one that answered only requests the specification does
 * not declare, whose spans the routing drops before this is asked. The calculation is refused in
 * all three - a ratio over the harness's spans alone would judge reach over a dataset the
 * application did not appear in - but an operator acting on the wrong one of them would go looking
 * for a producer that is already running.
 * <p>
 * A verdict span is admitted into the analysed dataset so that one criterion can read it, and
 * letting it decide that a calculation runs at all is the largest move it could make on another
 * criterion's verdict: without a producer configured the same window carries no spans and is
 * reported as no telemetry, while with one every criterion that judges reach would publish 0% over
 * a dataset the application never appeared in.
 * <p>
 * Checked, and declared out to the stream processor, for the reason
 * {@link UnmatchedWaiverException} records - and reported rather than answered with an empty result
 * set, because a calculation that publishes no result at all has no criterion below the gate's
 * threshold and would be read as a pass.
 */
@RealizesCon(ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT)
public class NoApplicationTelemetryException extends Exception {

  public NoApplicationTelemetryException(ApiInformation apiInformation) {
    super(
      "Every span attributed to a documented operation of { \"serviceName\": \"%s\", \"apiName\": \"%s\", \"apiVersion\": \"%s\" } was exported by a test harness, and none by the application under test. The application is not instrumented, its telemetry did not reach the configured backend, or it answered no request this specification declares; no coverage is reported for a window the application never appeared in.".formatted(
        apiInformation.getServiceName(),
        apiInformation.getApiName(),
        apiInformation.getApiVersion()
      )
    );
  }
}
