/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ApiInformationMapper {
  @Mapping(target = "apiName", source = "name")
  @Mapping(target = "apiVersion", source = "version")
  GetAllApis200ResponseInner toDto(ApiInformation apiInformation);
}
