#!/usr/bin/env node

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { Command } from 'commander';

import { calculate } from './actions/calculate';
import type { CalculateOptions } from './actions/calculate.options';
import { getQualityGateApi } from './api/quality-gate-api';

const program = new Command();

program
  .name('snow-white')
  .description('CLI tool to interact with Snow-White.')
  .version('1.0.0');

program
  .command('info')
  .description('Show system information')
  .action(() => {
    console.log(chalk.blue('System Information:'));
    console.log(`Platform: ${process.platform}`);
    console.log(`Architecture: ${process.arch}`);
    console.log(`Node.js version: ${process.version}`);
  });

program
  .command('calculate')
  .description('Trigger a Quality-Gate calculation')
  .requiredOption('--qualityGate <gateName>', 'Quality-Cate configuration name')
  .requiredOption('--serviceName <serviceName>', 'Service name')
  .requiredOption('--apiName <apiName>', 'API name')
  .requiredOption('--apiVersion <version>', 'API version')
  .option('--url <baseUrl>', 'Base URL for Snow-White', 'http://localhost:8090')
  .action(async (options: CalculateOptions)=>await calculate(getQualityGateApi(options.url), options));

program.parse();
