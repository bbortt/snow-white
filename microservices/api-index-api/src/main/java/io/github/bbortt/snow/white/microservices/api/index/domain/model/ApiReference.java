/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.model;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@IdClass(ApiReference.ApiReferenceId.class)
public class ApiReference {

  @Id
  @NonNull
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String otelServiceName;

  @Id
  @NonNull
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String apiName;

  @Id
  @NonNull
  @Size(min = 1, max = 16)
  @Column(nullable = false, updatable = false, length = 16)
  private String apiVersion;

  @NonNull
  @Size(min = 1, max = 512)
  @Column(nullable = false, updatable = false, length = 512)
  private String sourceUrl;

  @Enumerated(STRING)
  @Column(nullable = false, length = 16)
  private GetAllApis200ResponseInner.@NonNull ApiTypeEnum apiType;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ApiReferenceId implements Serializable {

    private String otelServiceName;
    private String apiName;
    private String apiVersion;
  }
}
