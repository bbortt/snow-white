#!/usr/bin/env node

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { Command } from 'commander';

import type { CliOptions } from './config/cli-options';

import { calculate } from './actions/calculate';
import { uploadPrereleases } from './actions/upload-prereleases';
import { getApiIndexApi } from './api/api-index-api';
import { getQualityGateApi } from './api/quality-gate-api';
import { getReportApi } from './api/report-api';
import {
  sanitizeCalculateOptions,
  sanitizeUploadPrereleasesOptions
  
} from './config/sanitize-configuration';

const program = new Command();

program
  .name('snow-white')
  .description('CLI tool to interact with Snow-White.')
  .version('1.0.0');

program
  .command('info')
  .description('Show system information')
  .action(() => {
    console.info(chalk.blue('System Information:'));
    console.info(`Platform: ${process.platform}`);
    console.info(`Architecture: ${process.arch}`);
    console.info(`Node.js version: ${process.version}`);
  });

program
  .command('calculate')
  .description('Trigger a Quality-Gate calculation')
  // Configuration from file, can contain all other configuration methods
  .option('--config-file <path>', 'Path to a YAML or JSON config file')
  // Read all OpenAPI specs matching the glob pattern
  .option('--open-api-specs <pattern>', 'Glob pattern selecting which OpenAPI spec files to use')
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
  .option('--filter <key=value>', 'Attribute filter for telemetry data (can be repeated)', (value:string, previous: string[]|undefined) => {
    return previous ? [...previous, value] : [value];
  }, [])
  .option('--async', 'Fire-and-forget: do not poll for the calculation result', false)
  .action(async (options: CliOptions) => {
    const sanitizedOptions = sanitizeCalculateOptions(options);
    const qualityGateApi = getQualityGateApi(sanitizedOptions.url);
    const reportApi = getReportApi(sanitizedOptions.url);
    await calculate(qualityGateApi, reportApi, sanitizedOptions);
  });

program
  .command('upload-prereleases')
  .description(
    'Upload one or more API specifications from the local file system as prereleases.\n' +
      'Intended to be called at the start of a pipeline before QA runs.\n' +
      'Uploaded prereleases are temporary and will be cleaned up asynchronously.',
  )
  .requiredOption('--prerelease-specs <pattern>', 'Glob pattern selecting which specification files to upload (e.g. "services/**/openapi.yaml")')
  .option('--url <baseUrl>', 'Base URL for Snow-White (overrides config file)')
  .option('--config-file <path>', 'Path to config file (used to resolve --url if not provided directly)')
  .option('--api-name-path <jsonPath>', 'JSON path to the API name field in the specification')
  .option('--api-version-path <jsonPath>', 'JSON path to the API version field in the specification')
  .option(
    '--service-name-path <jsonPath>',
    'JSON path to the service name field in the specification (maps to the x-service-name extension in raw YAML)',
  )
  .option('--ignore-existing', 'Ignore previously indexed API specifications', false)
  .action(async (options: CliOptions) => {
    const sanitizedOptions = sanitizeUploadPrereleasesOptions(options);
    const apiIndexApi = getApiIndexApi(sanitizedOptions.url);
    await uploadPrereleases(apiIndexApi, sanitizedOptions);
  });

program.parse();
