/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { cosmiconfigSync, type CosmiconfigResult } from 'cosmiconfig';
import chalk from 'chalk';
import { exit } from 'node:process';
import { CONFIG_FILE_NOT_FOUND, FAILED_LOADING_CONFIG_FILE } from './exit-codes';

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

export const resolveConfig = (
  filepath: string,
  resolver: ConfigResolver = new CosmiconfigResolver(),
  moduleName: string = 'snow-white',
): CosmiconfigResult => {
  const explorer = resolver.createExplorer(moduleName);

  console.log(`⚙️ Loading configuration file: ${filepath}`);

  try {
    const config = explorer.load(filepath);
    if (!config) {
      console.error(chalk.red(`⚙️ Configuration file not found at '${filepath}'`));
      exit(CONFIG_FILE_NOT_FOUND);
    }
    return config;
  } catch (error) {
    console.error(chalk.red(`⚙️ Failed to load configuration from '${filepath}':`), error);
    exit(FAILED_LOADING_CONFIG_FILE);
  }
};

export const resolveSnowWhiteConfig = <T>(filepath: string): T => resolveConfig(filepath).config as unknown as T;
