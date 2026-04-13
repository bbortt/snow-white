/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface CliOptions {
  configFile?: string;

  url?: string;

  openApiSpecs?: string;

  qualityGate?: string;
  serviceName?: string;
  apiName?: string;
  apiVersion?: string;

  lookbackWindow?: string;
  filter?: string[];

  async?: boolean;

  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;

  prereleaseSpecs: string;

  ignoreExisting?: boolean;
}
