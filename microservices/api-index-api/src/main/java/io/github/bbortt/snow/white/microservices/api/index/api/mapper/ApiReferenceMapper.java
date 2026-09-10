/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.mapper;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;

@Mapper(componentModel = SPRING)
public interface ApiReferenceMapper {
  @Mapping(target = "indexedAt", ignore = true)
  @Mapping(target = "otelServiceName", source = "serviceName")
  @Mapping(
    target = "prereleaseContent",
    source = "content",
    qualifiedBy = EmptyStringToNull.class
  )
  ApiReference fromDto(GetAllApis200ResponseInner apiEndpoint);

  @Mapping(target = "serviceName", source = "otelServiceName")
  @Mapping(target = "content", source = "prereleaseContent")
  GetAllApis200ResponseInner toDto(@NonNull ApiReference apiReference);

  @EmptyStringToNull
  default @Nullable String emptyStringToNull(@Nullable String s) {
    return hasText(s) ? s : null;
  }

  @Qualifier
  @Target(METHOD)
  @Retention(CLASS)
  @interface EmptyStringToNull {}
}
