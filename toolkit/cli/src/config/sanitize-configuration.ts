/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { load } from 'js-yaml';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { CliOptions } from './cli-options';
import type { ApiInformation, CalculateOptions, UploadPrereleasesOptions } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH, extractApiSpecMetadata } from '../common/openapi';
import { resolveConfig } from './resolve-config';

const exactConfigurationGroup = Object.freeze(['serviceName', 'apiName', 'apiVersion']);
const distinctConfigGroups = Object.freeze([exactConfigurationGroup, ['configFile']]);

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
const mergeWithCliOverrides = (fileConfig: Partial<CalculateOptions>, cliOptions: CliOptions): Partial<CalculateOptions> => {
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

const loadApiInformationFromGlob = (globPattern: string, options: CliOptions, fileConfig: CliOptions): ApiInformation[] => {
  const apiNamePath = options.apiNamePath ?? fileConfig.apiNamePath ?? DEFAULT_API_NAME_PATH;
  const apiVersionPath = options.apiVersionPath ?? fileConfig.apiVersionPath ?? DEFAULT_API_VERSION_PATH;
  const serviceNamePath = options.serviceNamePath ?? fileConfig.serviceNamePath ?? DEFAULT_SERVICE_NAME_PATH;

  const files = scanGlob(globPattern, process.cwd());

  if (files.length === 0) {
    console.warn(chalk.yellow(`⚠️  No files matched the pattern: ${globPattern}`));
    return [];
  }

  console.log(chalk.gray(`Found ${files.length} file(s) matching pattern: ${globPattern}`));

  const apiInformation: ApiInformation[] = [];
  let hasErrors = false;

  for (const file of files) {
    try {
      const content = readFileSync(file, 'utf8');
      const parsed = load(content);
      const result = extractApiSpecMetadata(parsed, { apiNamePath, apiVersionPath, serviceNamePath });

      if (!result.ok) {
        console.error(chalk.red(`❌  ${file}: Missing required metadata fields.`));
        result.missing.forEach(path => console.error(chalk.red(`\t  '${path}' not found or empty.`)));
        hasErrors = true;
        continue;
      }

      const { apiName, apiVersion, serviceName } = result.metadata;
      apiInformation.push({ apiName, apiVersion, serviceName });
      console.log(chalk.gray(`  ✓ ${file}: ${serviceName}/${apiName}@${apiVersion}`));
    } catch (error) {
      console.error(chalk.red(`❌  ${file}: Failed to parse file.`));
      console.error(chalk.red(`\t  Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
      hasErrors = true;
    }
  }

  if (hasErrors) {
    exitWithCodeInvalidConfig();
  }

  return apiInformation;
};

const loadConfigBasedOnType = (options: CliOptions): object => {
  // apiSpecs cannot be combined with exact config parameters
  if (options.apiSpecs && exactConfigurationGroup.some(opt => options[opt as keyof CliOptions])) {
    console.error(chalk.red('❌  You cannot use options from multiple configuration groups together.'));
    console.error(chalk.red(`\tGroup 1: ${exactConfigurationGroup.join(', ')}`));
    console.error(chalk.red('\tGroup 2: apiSpecs'));
    exitWithCodeInvalidConfig();
  }

  const activeGroups = distinctConfigGroups.map(group => group.some(opt => options[opt as keyof CliOptions])).filter(Boolean);

  if (activeGroups.length > 1) {
    console.error(chalk.red('❌  You cannot use options from multiple configuration groups together.'));
    distinctConfigGroups.forEach((group, idx) => {
      console.error(chalk.red(`\tGroup ${idx + 1}: ${group.join(', ')}`));
    });
    exitWithCodeInvalidConfig();
  }

  let loadedFileOptions: Partial<CliOptions> | undefined;
  let baseConfig: Partial<CalculateOptions>;

  if (activeGroups.length === 0) {
    if (options.apiSpecs) {
      // Only apiSpecs on CLI, no config file group — build base config from CLI options
      baseConfig = mergeWithCliOverrides({}, options);
    } else {
      loadedFileOptions = validateConfigurationFromFile(resolveConfig()) as unknown as Partial<CliOptions>;
      baseConfig = mergeWithCliOverrides(loadedFileOptions as Partial<CalculateOptions>, options);
    }
  } else if (options.configFile) {
    loadedFileOptions = validateConfigurationFromFile(resolveConfig(options.configFile)) as unknown as Partial<CliOptions>;
    baseConfig = mergeWithCliOverrides(loadedFileOptions as Partial<CalculateOptions>, options);
  } else {
    if (exactConfigurationGroup.some(opt => !Object.hasOwn(options, opt))) {
      console.error(chalk.red('❌  Either define a config file or all of these calculation parameters:'));
      exactConfigurationGroup.forEach(opt => console.error(chalk.red(`\t- --${opt.replaceAll(/([A-Z])/g, '-$1').toLowerCase()}`)));
      exitWithCodeInvalidConfig();
    }

    const { apiName, apiVersion, async, lookbackWindow, qualityGate, serviceName, url } = options;
    const attributeFilters = parseFilters(options.filter);

    baseConfig = {
      apiInformation: [{ apiName, apiVersion, serviceName }],
      async: async ?? false,
      attributeFilters,
      lookbackWindow,
      qualityGate,
      url,
    };
  }

  // Resolve effective apiSpecs: CLI takes precedence over config file
  const fileApiSpecs = loadedFileOptions?.apiSpecs;
  const effectiveApiSpecs = options.apiSpecs ?? fileApiSpecs;

  if (options.apiSpecs && fileApiSpecs && fileApiSpecs !== options.apiSpecs) {
    console.warn(chalk.yellow(`⚠️  CLI parameter --api-specs overrides config file value: "${fileApiSpecs}" → "${options.apiSpecs}"`));
  }

  // Apply apiSpecs overlay: if apiInformation is already present it takes precedence
  if (effectiveApiSpecs) {
    if (baseConfig.apiInformation && baseConfig.apiInformation.length > 0) {
      console.warn(chalk.yellow('⚠️  --api-specs is ignored because apiInformation is already defined in the configuration.'));
    } else {
      baseConfig.apiInformation = loadApiInformationFromGlob(effectiveApiSpecs, options, baseConfig);
    }
  }

  return baseConfig;
};

export const validateConfiguration = (config: CalculateOptions): CalculateOptions => {
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

export const sanitizeCalculateOptions = (options: CliOptions): CalculateOptions => {
  const config = loadConfigBasedOnType(options);
  return validateConfiguration(config as CalculateOptions);
};

export const sanitizeUploadPrereleasesOptions = (options: CliOptions): UploadPrereleasesOptions => {
  let fileConfig: Partial<CliOptions> = {};

  // Load config file when explicitly requested, when URL is missing from CLI, or when apiSpecs is missing from CLI
  if (options.configFile || !options.url || !options.apiSpecs) {
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

  // apiSpecs: CLI takes precedence over config file
  const fileApiSpecs = fileConfig.apiSpecs;
  const apiSpecs = options.apiSpecs ?? fileApiSpecs;

  if (options.apiSpecs && fileApiSpecs && fileApiSpecs !== options.apiSpecs) {
    console.warn(chalk.yellow(`⚠️  CLI parameter --api-specs overrides config file value: "${fileApiSpecs}" → "${options.apiSpecs}"`));
  }

  if (!apiSpecs) {
    console.error(chalk.red('❌  API specs glob pattern must be defined via --api-specs or in the configuration file.'));
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
    globPattern: apiSpecs,
    ignoreExisting: options.ignoreExisting ?? false,
    serviceNamePath,
    url,
  };
};
