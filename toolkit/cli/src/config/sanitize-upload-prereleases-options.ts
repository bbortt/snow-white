/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { CliOptions } from './cli-options';
import type { UploadPrereleasesOptions } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH } from '../common/openapi';
import { resolveConfig } from './resolve-config';

const warnOverride = (flag: string, fileValue: string, cliValue: string): void => {
  console.warn(chalk.yellow(`⚠️ CLI parameter ${flag} overrides config file value: "${fileValue}" → "${cliValue}"`));
};

const resolveOption = (flag: string, cliValue?: string, fileValue?: string, defaultValue?: string): string | undefined => {
  if (cliValue && fileValue && fileValue !== cliValue) {
    warnOverride(flag, fileValue, cliValue);
  }
  return cliValue ?? fileValue ?? defaultValue;
};

const requireOption = (value: string | undefined, message: string): string => {
  if (!value) {
    console.error(chalk.red(message));
    exit(INVALID_CONFIG_FORMAT);
  }
  return value;
};

export const sanitizeUploadPrereleasesOptions = (options: CliOptions): UploadPrereleasesOptions => {
  const needsConfig = !!options.configFile || !options.url || !options.apiSpecs;
  const fileConfig: Partial<CliOptions> = needsConfig ? (resolveConfig(options.configFile) as Partial<CliOptions>) : {};

  const url = requireOption(
    resolveOption('--url', options.url, fileConfig.url),
    '❌ Snow-White base URL must be defined via --url or in the configuration file.',
  );

  const apiSpecs = requireOption(
    resolveOption('--api-specs', options.apiSpecs, fileConfig.apiSpecs),
    '❌ API specs glob pattern must be defined via --api-specs or in the configuration file.',
  );

  return {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    apiNamePath: resolveOption('--api-name-path', options.apiNamePath, fileConfig.apiNamePath, DEFAULT_API_NAME_PATH)!,
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    apiVersionPath: resolveOption('--api-version-path', options.apiVersionPath, fileConfig.apiVersionPath, DEFAULT_API_VERSION_PATH)!,
    globPattern: apiSpecs,
    ignoreExisting: options.ignoreExisting ?? false,
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    serviceNamePath: resolveOption('--service-name-path', options.serviceNamePath, fileConfig.serviceNamePath, DEFAULT_SERVICE_NAME_PATH)!,
    url,
  };
};
