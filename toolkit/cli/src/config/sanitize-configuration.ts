/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { UploadPrereleasesOptions } from '../actions/upload-prereleases';
import type { CliOptions } from './cli-options';
import type { SanitizedOptions } from './sanitized-options';

import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH } from '../actions/upload-prereleases';
import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { resolveConfig } from './resolve-config';

const exactConfigurationGroup = Object.freeze(['serviceName', 'apiName', 'apiVersion']);
const distinctConfigGroups = Object.freeze([exactConfigurationGroup, ['configFile'], ['openApiSpecs']]);

const exitWithCodeInvalidConfig = (): void => {
  exit(INVALID_CONFIG_FORMAT);
};

/**
 * Parses filter strings in format "key=value" into a record.
 */
const parseFilters = (filters?: string[]): Record<string, string> | undefined => {
  if (!filters || filters.length === 0) {
    return undefined;
  }

  const result: Record<string, string> = {};
  for (const filter of filters) {
    const separatorIndex = filter.indexOf('=');
    if (separatorIndex === -1) {
      console.warn(chalk.yellow(`⚠️  Ignoring invalid filter format: "${filter}". Expected format: key=value`));
      continue;
    }

    const key = filter.slice(0, separatorIndex).trim();
    const value = filter.slice(separatorIndex + 1).trim();

    if (!key || !value) {
      console.warn(chalk.yellow(`⚠️  Ignoring invalid filter: "${filter}". Both key and value must be non-empty.`));
      continue;
    }

    result[key] = value;
  }

  return Object.keys(result).length > 0 ? result : undefined;
};

/**
 * Merges CLI options over file config, with CLI taking precedence.
 * Logs warnings when CLI options override file config values.
 */
const mergeWithCliOverrides = (fileConfig: Partial<SanitizedOptions>, cliOptions: CliOptions): Partial<SanitizedOptions> => {
  const result = { ...fileConfig };

  // URL override
  if (cliOptions.url !== undefined) {
    if (fileConfig.url !== undefined && fileConfig.url !== cliOptions.url) {
      console.warn(chalk.yellow(`⚠️  CLI parameter --url overrides config file value: "${fileConfig.url}" → "${cliOptions.url}"`));
    }
    result.url = cliOptions.url;
  }

  // Quality gate override
  if (cliOptions.qualityGate !== undefined) {
    if (fileConfig.qualityGate !== undefined && fileConfig.qualityGate !== cliOptions.qualityGate) {
      console.warn(
        chalk.yellow(
          `⚠️  CLI parameter --quality-gate overrides config file value: "${fileConfig.qualityGate}" → "${cliOptions.qualityGate}"`,
        ),
      );
    }
    result.qualityGate = cliOptions.qualityGate;
  }

  // Lookback window override
  if (cliOptions.lookbackWindow !== undefined) {
    if (fileConfig.lookbackWindow !== undefined && fileConfig.lookbackWindow !== cliOptions.lookbackWindow) {
      console.warn(
        chalk.yellow(
          `⚠️  CLI parameter --lookback-window overrides config file value: "${fileConfig.lookbackWindow}" → "${cliOptions.lookbackWindow}"`,
        ),
      );
    }
    result.lookbackWindow = cliOptions.lookbackWindow;
  }

  // Filter override
  const cliFilters = parseFilters(cliOptions.filter);
  if (cliFilters !== undefined) {
    if (fileConfig.attributeFilters !== undefined && Object.keys(fileConfig.attributeFilters).length > 0) {
      console.warn(chalk.yellow(`⚠️  CLI parameter --filter overrides config file attributeFilters`));
    }
    result.attributeFilters = cliFilters;
  }

  // Async flag: CLI-only, not read from config file
  if (cliOptions.async !== undefined) {
    result.async = cliOptions.async;
  }

  return result;
};

const validateConfigurationFromFile = (options: CliOptions): CliOptions => {
  if (Object.keys(options).length === 0 || options.configFile) {
    console.error(chalk.red('❌  Configuration file may not contain recursive references or be empty.'));
    console.error(chalk.red("\tHere's an example of a valid configuration file:"));
    console.error();
    console.error(
      chalk.red(
        JSON.stringify(
          {
            apiInformation: [{ apiName: 'ping-pong', apiVersion: '1.0.0', serviceName: 'example-application' }],
            attributeFilters: {
              environment: 'production',
            },
            lookbackWindow: '1h',
            qualityGate: 'basic-coverage',
            url: 'http://localhost:9000',
          },
          null,
          2,
        ),
      ),
    );
    exitWithCodeInvalidConfig();
  }

  return options;
};

const loadConfigBasedOnType = (options: CliOptions): object => {
  const activeGroups = distinctConfigGroups.map(group => group.some(opt => options[opt as keyof CliOptions])).filter(Boolean);

  if (activeGroups.length > 1) {
    console.error(chalk.red('❌  You cannot use options from multiple configuration groups together.'));
    distinctConfigGroups.forEach((group, idx) => {
      console.error(chalk.red(`\tGroup ${idx + 1}: ${group.join(', ')}`));
    });
    exitWithCodeInvalidConfig();
  }

  if (activeGroups.length === 0) {
    const fileConfig = validateConfigurationFromFile(resolveConfig()) as Partial<SanitizedOptions>;
    return mergeWithCliOverrides(fileConfig, options);
  }
  if (options.configFile) {
    const fileConfig = validateConfigurationFromFile(resolveConfig(options.configFile)) as Partial<SanitizedOptions>;
    return mergeWithCliOverrides(fileConfig, options);
  }

  if (options.openApiSpecs) {
    // TODO: Load OpenAPI specs and merge them into options
    console.warn(chalk.yellow('⚠️  OpenAPI specs are not yet implemented. Using provided options as is.'));
    exit(0);
  }

  if (exactConfigurationGroup.some(opt => !Object.hasOwn(options, opt))) {
    console.error(chalk.red('❌  Either define a config file or all of these calculation parameters:'));
    exactConfigurationGroup.forEach(opt => console.error(chalk.red(`\t- --${opt.replaceAll(/([A-Z])/g, '-$1').toLowerCase()}`)));
    exitWithCodeInvalidConfig();
  }

  const { apiName, apiVersion, async, lookbackWindow, qualityGate, serviceName, url } = options;
  const attributeFilters = parseFilters(options.filter);

  return {
    apiInformation: [{ apiName, apiVersion, serviceName }],
    async: async ?? false,
    attributeFilters,
    lookbackWindow,
    qualityGate,
    url,
  };
};

export const validateConfiguration = (config: SanitizedOptions): SanitizedOptions => {
  if (!config.url) {
    console.error(chalk.red('❌  Snow-White base URL must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
  }
  if (!config.qualityGate) {
    console.error(chalk.red('❌  Quality-Gate name must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  } else if (!config.apiInformation || config.apiInformation.length === 0) {
    console.error(chalk.red('❌  At least one API information must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
  } else if (config.apiInformation.some(api => !api.serviceName || !api.apiName || !api.apiVersion)) {
    console.error(chalk.red('❌  Each API information must contain serviceName, apiName, and apiVersion.'));
    exitWithCodeInvalidConfig();
  }

  // Validate lookbackWindow format if provided
  if (config.lookbackWindow !== undefined) {
    const lookbackPattern = /^\d+[hdwm]$/i;
    if (!lookbackPattern.test(config.lookbackWindow)) {
      console.warn(
        chalk.yellow(
          `⚠️  Lookback window "${config.lookbackWindow}" may not be in expected format (e.g., '1h', '24h', '7d'). Proceeding anyway.`,
        ),
      );
    }
  }

  return config;
};

export const sanitizeCalculateOptions = (options: CliOptions): SanitizedOptions => {
  const config = loadConfigBasedOnType(options);
  return validateConfiguration(config as SanitizedOptions);
};

export interface UploadCliOptions {
  prereleaseSpecs: string;
  url?: string;
  configFile?: string;
  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;
  ignoreExisting?: boolean;
}

export const sanitizeUploadPrereleasesOptions = (options: UploadCliOptions): UploadPrereleasesOptions => {
  let fileConfig: Partial<CliOptions> = {};

  // Load config file when explicitly requested or when URL is not provided via CLI
  if (options.configFile || !options.url) {
    fileConfig = resolveConfig(options.configFile) as Partial<CliOptions>;
  }

  // URL: CLI takes precedence over config file
  let url: string | undefined = options.url;
  if (!url) {
    url = fileConfig.url;
  } else if (fileConfig.url && fileConfig.url !== url) {
    console.warn(chalk.yellow(`⚠️  CLI parameter --url overrides config file value: "${fileConfig.url}" → "${url}"`));
  }

  if (!url) {
    console.error(chalk.red('❌  Snow-White base URL must be defined via --url or in the configuration file.'));
    exit(INVALID_CONFIG_FORMAT);
  }

  // Path params: CLI > config file > hardcoded defaults
  const apiNamePath = options.apiNamePath ?? fileConfig.apiNamePath ?? DEFAULT_API_NAME_PATH;
  const apiVersionPath = options.apiVersionPath ?? fileConfig.apiVersionPath ?? DEFAULT_API_VERSION_PATH;
  const serviceNamePath = options.serviceNamePath ?? fileConfig.serviceNamePath ?? DEFAULT_SERVICE_NAME_PATH;

  if (options.apiNamePath && fileConfig.apiNamePath && fileConfig.apiNamePath !== options.apiNamePath) {
    console.warn(
      chalk.yellow(`⚠️  CLI parameter --api-name-path overrides config file value: "${fileConfig.apiNamePath}" → "${options.apiNamePath}"`),
    );
  }
  if (options.apiVersionPath && fileConfig.apiVersionPath && fileConfig.apiVersionPath !== options.apiVersionPath) {
    console.warn(
      chalk.yellow(
        `⚠️  CLI parameter --api-version-path overrides config file value: "${fileConfig.apiVersionPath}" → "${options.apiVersionPath}"`,
      ),
    );
  }
  if (options.serviceNamePath && fileConfig.serviceNamePath && fileConfig.serviceNamePath !== options.serviceNamePath) {
    console.warn(
      chalk.yellow(
        `⚠️  CLI parameter --service-name-path overrides config file value: "${fileConfig.serviceNamePath}" → "${options.serviceNamePath}"`,
      ),
    );
  }

  return {
    apiNamePath,
    apiVersionPath,
    globPattern: options.prereleaseSpecs,
    ignoreExisting: options.ignoreExisting ?? false,
    serviceNamePath,
    url,
  };
};
