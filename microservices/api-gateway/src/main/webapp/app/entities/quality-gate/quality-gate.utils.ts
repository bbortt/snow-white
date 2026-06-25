/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface QualityGateFilterParams {
  serviceName: string;
  apiName: string;
  apiVersion: string;
}

export function extractQualityGateFilterParams(search: string): QualityGateFilterParams {
  const params = new URLSearchParams(search);
  return {
    serviceName: params.get('serviceName') ?? '',
    apiName: params.get('apiName') ?? '',
    apiVersion: params.get('apiVersion') ?? '',
  };
}

export function countActiveFilters(params: QualityGateFilterParams): number {
  return [params.serviceName, params.apiName, params.apiVersion].filter(Boolean).length;
}
