/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.Map;

class YamlParser {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
    new YAMLFactory()
  );

  String readSpecToJson(String inputSpec) {
    try {
      Map<String, Object> yamlData = YAML_MAPPER.readValue(
        new File(inputSpec),
        Map.class
      );

      return new ObjectMapper().writeValueAsString(yamlData);
    } catch (IOException e) {
      throw new YamlParsingException(e);
    }
  }
}
