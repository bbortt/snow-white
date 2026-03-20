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
import { getQualityGateApi } from './api/quality-gate-api';
import { sanitizeConfiguration } from './config/sanitize-configuration';

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
  .option('--configFile <path>', 'Path to config file')
  // Read all OpenAPI specs matching the glob pattern
  .option('--openApiSpecs <pattern>', 'Glob pattern for OpenAPI specs')
  // Explicit configuration for the Quality-Gate calculation
  .option('--qualityGate <gateName>', 'Quality-Gate configuration name')
  .option('--serviceName <serviceName>', 'Service name')
  .option('--apiName <apiName>', 'API name')
  .option('--apiVersion <version>', 'API version')
  .option('--url <baseUrl>', 'Base URL for Snow-White (overrides config file)')
  // Filter and lookback options
  .option('--lookbackWindow <window>', "Time window for calculation (e.g., '1h', '24h', '7d')")
  .option('--filter <key=value>', 'Attribute filter for telemetry data (can be repeated)', (value:string, previous: string[]|undefined) => {
    return previous ? [...previous, value] : [value];
  }, [])
  .action(async (options: CliOptions) => {
    const sanitizedOptions = sanitizeConfiguration(options);
    const qualityGateApi = getQualityGateApi(sanitizedOptions.url);
    await calculate(qualityGateApi, sanitizedOptions);
  });

program.parse();
