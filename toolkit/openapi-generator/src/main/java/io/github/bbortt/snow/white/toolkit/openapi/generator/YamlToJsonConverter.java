/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import java.io.File;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

final class YamlToJsonConverter {

  String readSpecToJson(String inputSpec) {
    var yamlData = YAMLMapper.shared().readValue(
      new File(inputSpec),
      Map.class
    );

    return JsonMapper.shared().writeValueAsString(yamlData);
  }
}
