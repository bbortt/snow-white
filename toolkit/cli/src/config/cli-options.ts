/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface CliOptions {
  /**
   * Configuration from file
   */
  configFile?: string;
  /**
   * Resolving API specifications
   */
  openApiSpecs?: string;
  // Explicit configuration
  qualityGate?: string;
  serviceName?: string;
  apiName?: string;
  apiVersion?: string;
  /**
   * Base URL for Snow-White API.
   */
  url: string;
}
