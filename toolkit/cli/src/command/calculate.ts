/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { Command } from 'commander';

import type { CliOptions } from '../config/cli-options';

import { calculate as calculateAction } from '../action/calculate';
import { getQualityGateApi } from '../api/quality-gate-api';
import { getReportApi } from '../api/report-api';
import { sanitizeCalculateOptions } from '../config/sanitize-calculate-options';

export const calculate = (program: Command): void => {
  program
    .command('calculate')
    .description('Trigger a Quality-Gate calculation')
    // Configuration from file, can contain all other configuration methods
    .option('--config-file <path>', 'Path to a YAML or JSON config file')
    // Read all OpenAPI specs matching the glob pattern
    .option('--api-specs <pattern>', 'Glob pattern selecting which OpenAPI spec files to use')
    .option('--api-name-path <jsonPath>', 'JSON path to the API name field in the specification (default: info.title)')
    .option('--api-version-path <jsonPath>', 'JSON path to the API version field in the specification (default: info.version)')
    .option('--service-name-path <jsonPath>', 'JSON path to the service name field in the specification (default: info.x-service-name)')
    // Explicit configuration for the Quality-Gate calculation
    .option('--quality-gate <name>', 'Quality-Gate configuration name')
    .option('--service-name <name>', 'Name of the service')
    .option('--api-name <name>', 'Name of the API')
    .option('--api-version <version>', 'API version')
    .option('--url <baseUrl>', 'Base URL for Snow-White (overrides config file)')
    // Filter and lookback options
    .option('--lookback-window <window>', "Time window for calculation (e.g., '1h', '24h', '7d')")
    .option(
      '--filter <key=value>',
      'Attribute filter for telemetry data (can be repeated)',
      (value: string, previous: string[] | undefined) => {
        return previous ? [...previous, value] : [value];
      },
      [],
    )
    .option('--async', 'Fire-and-forget: do not poll for the calculation result', false)
    .option('--junit-output <path>', 'Write the JUnit XML report to the given file path (cannot be combined with --async)')
    .action(async (options: CliOptions) => {
      const sanitizedOptions = sanitizeCalculateOptions(options);
      const qualityGateApi = getQualityGateApi(sanitizedOptions.url);
      const reportApi = getReportApi(sanitizedOptions.url);
      await calculateAction(qualityGateApi, reportApi, sanitizedOptions);
    });
};
