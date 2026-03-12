/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface ApiTypeMapper {
  Short toEntity(GetAllApis200ResponseInner.ApiTypeEnum apiType);
}
