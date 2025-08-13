/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import chalk from 'chalk';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import type { CliOptions } from './cli-options';
import { resolveConfig } from './resolve-config';
import type { SanitizedOptions } from './sanitized-options';

const distinctConfigGroups = Object.freeze([['qualityGate', 'serviceName', 'apiName', 'apiVersion'], ['configFile'], ['openApiSpecs']]);

const printDistinctConfigGroups = (): void => {
  distinctConfigGroups.forEach((group, idx) => {
    console.error(chalk.red(`\tGroup ${idx + 1}: ${group.join(', ')}`));
  });
};

const exitWithCodeInvalidConfig = (): void => {
  exit(INVALID_CONFIG_FORMAT);
};

export const sanitizeConfiguration = (options: CliOptions): SanitizedOptions => {
  const activeGroups = distinctConfigGroups.map(group => group.some(opt => options[opt as keyof CliOptions])).filter(Boolean);

  if (activeGroups.length > 1) {
    console.error(chalk.red('❌ You cannot use options from multiple configuration groups together.'));
    printDistinctConfigGroups();
    exitWithCodeInvalidConfig();
  }

  if (activeGroups.length === 0) {
    console.error(chalk.red('❌ You must define at least one complete configuration group.'));
    printDistinctConfigGroups();
    exit(INVALID_CONFIG_FORMAT);
  }

  if (options.config) {
    return sanitizeConfiguration(resolveConfig(options.config));
  }

  return options;
};
