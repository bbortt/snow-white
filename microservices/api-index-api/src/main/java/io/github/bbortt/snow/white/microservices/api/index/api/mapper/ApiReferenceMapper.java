/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import org.jspecify.annotations.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ApiReferenceMapper {
  @Mapping(target = "otelServiceName", source = "serviceName")
  ApiReference fromDto(GetAllApis200ResponseInner apiEndpoint);

  @Mapping(target = "serviceName", source = "otelServiceName")
  GetAllApis200ResponseInner toDto(@NonNull ApiReference apiReference);
}
