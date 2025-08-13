/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import chalk from 'chalk';
import type { CosmiconfigResult } from 'cosmiconfig';
import { cosmiconfigSync } from 'cosmiconfig';

import { CONFIG_FILE_NOT_FOUND, FAILED_LOADING_CONFIG_FILE } from '../common/exit-codes';
import type { CliOptions } from './cli-options';

export interface ConfigExplorer {
  load(filepath: string): CosmiconfigResult;
}

export interface ConfigResolver {
  createExplorer(moduleName: string): ConfigExplorer;
}

export class CosmiconfigResolver implements ConfigResolver {
  createExplorer(moduleName: string): ConfigExplorer {
    return cosmiconfigSync(moduleName);
  }
}

/**
 * !!Visible for testing!!
 */
export const resolveConfigInternal = (
  filepath: string,
  resolver: ConfigResolver = new CosmiconfigResolver(),
  moduleName = 'snow-white',
): CosmiconfigResult => {
  const explorer = resolver.createExplorer(moduleName);

  console.log(`⚙️ Loading configuration file: ${filepath}`);

  try {
    const config = explorer.load(filepath);

    if (config) {
      return config;
    }

    console.error(chalk.red(`⚙️ Configuration file not found at '${filepath}'`));
    exit(CONFIG_FILE_NOT_FOUND);
  } catch (error) {
    console.error(chalk.red(`⚙️ Failed to load configuration from '${filepath}':`), error);
    exit(FAILED_LOADING_CONFIG_FILE);
  }
};

export const resolveConfig = (filepath: string): CliOptions => resolveConfigInternal(filepath).config as unknown as CliOptions;
