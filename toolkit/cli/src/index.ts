#!/usr/bin/env node

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { Command } from 'commander';

import { calculate } from './command/calculate';
import { uploadPrereleases } from './command/upload-prereleases';

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

calculate(program)
uploadPrereleases(program)

program.parse();
