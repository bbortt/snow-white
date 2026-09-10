/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import java.util.List;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class MaskingBeanSerializerModifier extends ValueSerializerModifier {

  @Override
  public List<BeanPropertyWriter> changeProperties(
    SerializationConfig config,
    BeanDescription.Supplier beanDesc,
    List<BeanPropertyWriter> beanProperties
  ) {
    for (BeanPropertyWriter writer : beanProperties) {
      if (SensitiveKeys.KEYS.contains(writer.getName())) {
        writer.assignSerializer(new MaskingSerializer());
      }
    }

    return beanProperties;
  }
}
