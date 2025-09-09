/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { existsSync } from 'node:fs';
import { exit } from 'node:process';

import chalk from 'chalk';
import type { CosmiconfigResult } from 'cosmiconfig';
import { cosmiconfigSync } from 'cosmiconfig';
import type { Config } from 'cosmiconfig/dist/types';

import { CONFIG_FILE_NOT_FOUND, FAILED_LOADING_CONFIG_FILE } from '../common/exit-codes';
import type { CliOptions } from './cli-options';

export interface ConfigExplorer {
  load(filepath: string): CosmiconfigResult;
  search(): CosmiconfigResult;
}

export interface ConfigResolver {
  createExplorer(moduleName: string): ConfigExplorer;
}

export class CosmiconfigResolver implements ConfigResolver {
  createExplorer(moduleName: string): ConfigExplorer {
    return cosmiconfigSync(moduleName);
  }
}

const resolveConfigFromFile = (filepath: string, explorer: ConfigExplorer): CosmiconfigResult => {
  console.log(`⚙️ Loading configuration file: ${filepath}`);
  const config = explorer.load(filepath);

  if (config) {
    return config;
  }

  console.error(chalk.red(`⚙️ Configuration file not found at '${filepath}'`));
  exit(CONFIG_FILE_NOT_FOUND);
};

/**
 * !!Visible for testing!!
 */
export const resolveConfigInternal = (
  filepath?: string,
  resolver: ConfigResolver = new CosmiconfigResolver(),
  moduleName = 'snow-white',
): Config => {
  const explorer = resolver.createExplorer(moduleName);

  if (!filepath) {
    filepath = explorer.search()?.filepath;
  }

  if (!filepath) {
    console.error(chalk.red(`⚙️ Failed to find configuration file - try with '--configFile <path-to-your-config-file>'`));
    exit(CONFIG_FILE_NOT_FOUND);
  } else if (filepath && !existsSync(filepath)) {
    console.error(chalk.red(`⚙️ Configuration file '${filepath}' does not exist`));
    exit(CONFIG_FILE_NOT_FOUND);
  }

  try {
    const config = resolveConfigFromFile(filepath, explorer);
    if (config) {
      return config.config;
    }

    console.error(chalk.red(`Configuration file '${filepath}' could not be loaded`));
    exit(FAILED_LOADING_CONFIG_FILE);
  } catch (error) {
    console.error(chalk.red(`⚙️ Failed to load configuration file: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
    exit(FAILED_LOADING_CONFIG_FILE);
  }
};

export const resolveConfig = (filepath?: string): CliOptions => resolveConfigInternal(filepath) as unknown as CliOptions;
