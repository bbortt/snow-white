/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { CalculateQualityGateRequestIncludeApisInner } from '../../clients/quality-gate-api';
import type { ApiInformation } from '../../config/sanitized-options';

export const toDtos = (apiInformation: ApiInformation[]): CalculateQualityGateRequestIncludeApisInner[] => {
  return apiInformation.map(api => ({
    serviceName: api.serviceName,
    apiName: api.apiName,
    apiVersion: api.apiVersion,
  }));
};
