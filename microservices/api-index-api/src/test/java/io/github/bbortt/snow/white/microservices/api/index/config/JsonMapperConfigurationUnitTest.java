/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonMapperConfigurationUnitTest {

  private JsonMapperConfiguration fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new JsonMapperConfiguration();
  }

  @Nested
  class OmitNullPropertiesCustomizerTest {

    record Payload(
      String present,
      String absent,
      List<String> empty,
      Map<String, String> nested
    ) {}

    @Test
    @VerifiesCon(ConTraceables.CON_010_REST_RESPONSES_NEVER_CARRY_NULL)
    void shouldOmitNullValues_butKeepEmptyOnes() {
      var builder = JsonMapper.builder();
      fixture.omitNullPropertiesCustomizer().customize(builder);

      var jsonMapper = builder.build();
      var json = jsonMapper.readTree(
        jsonMapper.writeValueAsString(
          new Payload("value", null, emptyList(), singletonMap("key", null))
        )
      );

      assertThat(json.get("present").asString()).isEqualTo("value");
      assertThat(json.has("absent")).isFalse();
      assertThat(json.get("empty").isArray()).isTrue();
      assertThat(json.get("empty").isEmpty()).isTrue();
      assertThat(json.get("nested").has("key")).isFalse();
    }
  }
}
