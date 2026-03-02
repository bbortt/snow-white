/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.BeanSerializerFactory;

public class LoggingJsonMapper {

  private static final ObjectMapper MASKING_JSON_MAPPER = JsonMapper.builder()
    .serializerFactory(
      BeanSerializerFactory.instance.withSerializerModifier(
        new MaskingBeanSerializerModifier()
      )
    )
    .build();

  public static String toMaskedJsonRepresentation(Object value) {
    try {
      return MASKING_JSON_MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      return "<serialization failed>";
    }
  }
}
