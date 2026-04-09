/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
  url?: string;
  /**
   * The time window to consider for calculation (e.g., '1h', '24h', '7d').
   */
  lookbackWindow?: string;
  /**
   * Key-value pairs for filtering telemetry data (format: key=value).
   * Can be specified multiple times.
   */
  filter?: string[];
  /**
   * Fire-and-forget mode: skip polling for the calculation result.
   * When false (default), the CLI polls until the calculation completes.
   */
  async?: boolean;
}
