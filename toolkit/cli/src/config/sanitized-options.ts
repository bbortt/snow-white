/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface ApiInformation {
  serviceName: string;
  apiName: string;
  apiVersion: string;
}

export interface SanitizedOptions {
  apiInformation: ApiInformation[];
  qualityGate: string;
  url: string;
  /**
   * The time window to consider for calculation (e.g., '1h', '24h', '7d').
   */
  lookbackWindow?: string;
  /**
   * Key-value map of attributes to filter telemetry data.
   */
  attributeFilters?: Record<string, string>;
}
