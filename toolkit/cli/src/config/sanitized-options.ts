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

/**
 * One criterion declared inapplicable to one target of one API. Read from the configuration file
 * only - there is deliberately no command-line flag for it, so a waiver is always a reviewed change.
 */
export interface Waiver {
  serviceName: string;
  apiName: string;
  apiVersion: string;
  path: string;
  method: string;
  responseCode?: string;
  parameterName?: string;
  contentType?: string;
  criterion?: string;
  reason: string;
  expires: Date;
}

export interface CalculateOptions {
  apiInformation: ApiInformation[];
  waivers?: Waiver[];
  async?: boolean;
  agentic: boolean;
  junitOutput?: string;
  reportOutput?: string;
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
