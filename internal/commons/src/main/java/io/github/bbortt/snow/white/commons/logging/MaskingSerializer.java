/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class MaskingSerializer extends StdSerializer<Object> {

  protected MaskingSerializer() {
    super(Object.class);
  }

  @Override
  public void serialize(
    Object value,
    JsonGenerator gen,
    SerializationContext ctxt
  ) throws JacksonException {
    gen.writeString("***");
  }
}
