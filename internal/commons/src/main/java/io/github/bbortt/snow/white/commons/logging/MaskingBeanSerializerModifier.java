package io.github.bbortt.snow.white.commons.logging;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
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
