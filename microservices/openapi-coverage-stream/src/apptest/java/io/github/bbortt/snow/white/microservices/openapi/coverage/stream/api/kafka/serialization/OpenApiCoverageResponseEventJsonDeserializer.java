/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import org.apache.kafka.common.serialization.Deserializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mirrors the deserialization side of {@link QualityGateCalculationEventSerdes} so the AppTest consumes the real response event type instead of parsing raw JSON by hand.
 */
public class OpenApiCoverageResponseEventJsonDeserializer
  implements Deserializer<OpenApiCoverageResponseEvent>
{

  @Override
  public OpenApiCoverageResponseEvent deserialize(String topic, byte[] data) {
    return JsonMapper.shared().readValue(
      data,
      OpenApiCoverageResponseEvent.class
    );
  }
}
