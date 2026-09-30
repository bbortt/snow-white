/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapperConfiguration {

  @Bean
  @RealizesCon(ConTraceables.CON_010_REST_RESPONSES_NEVER_CARRY_NULL)
  public JsonMapperBuilderCustomizer omitNullPropertiesCustomizer() {
    return builder ->
      builder.changeDefaultPropertyInclusion(incl ->
        incl.withValueInclusion(NON_NULL).withContentInclusion(NON_NULL)
      );
  }
}
