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

export interface CalculateOptions {
  apiInformation: ApiInformation[];
  async?: boolean;
  attributeFilters?: Record<string, string>;
  lookbackWindow?: string;
  globPattern: string;
  qualityGate: string;
  url: string;
}

export interface UploadPrereleasesOptions {
  globPattern: string;
  url: string;
  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;
  ignoreExisting?: boolean;
}
