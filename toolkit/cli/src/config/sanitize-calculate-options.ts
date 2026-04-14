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
import type { ApiInformation, CalculateOptions } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH, extractApiSpecMetadata } from '../common/openapi';
import { parseFilterObjectFromString } from './parse-filter-object-from-string.ts';
import { resolveConfig } from './resolve-config';

const exactConfigurationGroup = Object.freeze(['serviceName', 'apiName', 'apiVersion']);
const distinctConfigGroups = Object.freeze([exactConfigurationGroup, ['configFile']]);

const EXAMPLE_CONFIG = JSON.stringify(
  {
    apiSpecs: 'specs-folder/*.yml',
    attributeFilters: { environment: 'production' },
    lookbackWindow: '1h',
    qualityGate: 'basic-coverage',
    url: 'http://localhost:9000',
  },
  null,
  2,
);

const LOOKBACK_WINDOW_PATTERN = /^\d+[hdwm]$/i;

const exitInvalidConfig = (): never => exit(INVALID_CONFIG_FORMAT);

const warnOverride = (flag: string, fileValue: unknown, cliValue: unknown): void => {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  console.warn(chalk.yellow(`⚠️ CLI parameter ${flag} overrides config file value: "${fileValue}" → "${cliValue}"`));
};

const applyScalarOverride = <K extends keyof CalculateOptions>(
  result: Partial<CalculateOptions>,
  key: K,
  flag: string,
  cliValue: CalculateOptions[K] | undefined,
  fileValue: CalculateOptions[K] | undefined,
): void => {
  if (cliValue === undefined) {
    return;
  }
  if (fileValue !== undefined && fileValue !== cliValue) {
    warnOverride(flag, fileValue, cliValue);
  }
  result[key] = cliValue;
};

const mergeWithCliOverrides = (fileConfig: Partial<CalculateOptions>, cliOptions: CliOptions): Partial<CalculateOptions> => {
  const result: Partial<CalculateOptions> = { ...fileConfig };

  applyScalarOverride(result, 'url', '--url', cliOptions.url, fileConfig.url);
  applyScalarOverride(result, 'qualityGate', '--quality-gate', cliOptions.qualityGate, fileConfig.qualityGate);
  applyScalarOverride(result, 'lookbackWindow', '--lookback-window', cliOptions.lookbackWindow, fileConfig.lookbackWindow);

  const cliFilters = parseFilterObjectFromString(cliOptions.filter);
  if (cliFilters !== undefined) {
    if (fileConfig.attributeFilters && Object.keys(fileConfig.attributeFilters).length > 0) {
      console.warn(chalk.yellow('⚠️ CLI parameter --filter overrides config file attributeFilters'));
    }
    result.attributeFilters = cliFilters;
  }

  // async is CLI-only — never read from config file
  if (cliOptions.async !== undefined) {
    result.async = cliOptions.async;
  }

  return result;
};

const assertFileConfigIsValid = (options: CliOptions): void => {
  if (Object.keys(options).length === 0 || options.configFile) {
    console.error(chalk.red('❌ Configuration file may not contain recursive references or be empty.'));
    console.error(chalk.red("\tHere's an example of a valid configuration file:"));
    console.error();
    console.error(chalk.red(EXAMPLE_CONFIG));
    exitInvalidConfig();
  }
};

const loadFileConfig = (configFile?: string): CliOptions => {
  const loaded = resolveConfig(configFile);
  assertFileConfigIsValid(loaded);
  return loaded;
};

interface PathOptions {
  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;
}

const loadApiInformationFromGlob = (globPattern: string, fileOptions: CliOptions, cliOptions: PathOptions): ApiInformation[] => {
  const apiNamePath = cliOptions.apiNamePath ?? fileOptions.apiNamePath ?? DEFAULT_API_NAME_PATH;
  const apiVersionPath = cliOptions.apiVersionPath ?? fileOptions.apiVersionPath ?? DEFAULT_API_VERSION_PATH;
  const serviceNamePath = cliOptions.serviceNamePath ?? fileOptions.serviceNamePath ?? DEFAULT_SERVICE_NAME_PATH;

  const files = scanGlob(globPattern, process.cwd());

  if (files.length === 0) {
    console.warn(chalk.yellow(`⚠️ No files matched the pattern: ${globPattern}`));
    return [];
  }

  console.debug(chalk.gray(`\t  Found ${files.length} file(s) matching pattern: ${globPattern}`));

  const apiInformation: ApiInformation[] = [];
  let hasErrors = false;

  for (const file of files) {
    try {
      const parsed = load(readFileSync(file, 'utf8'));
      const result = extractApiSpecMetadata(parsed, { apiNamePath, apiVersionPath, serviceNamePath });

      if (!result.ok) {
        console.error(chalk.red(`❌ ${file}: Missing required metadata fields.`));
        result.missing.forEach(path => console.error(chalk.red(`\t  '${path}' not found or empty.`)));
        hasErrors = true;
        continue;
      }

      const { apiName, apiVersion, serviceName } = result.metadata;
      apiInformation.push({ apiName, apiVersion, serviceName });
      console.log(chalk.gray(`\t  ✓ ${file}: ${serviceName}/${apiName}@${apiVersion}`));
    } catch (error) {
      console.error(chalk.red(`❌ ${file}: Failed to parse file.`));
      console.error(chalk.red(`\t  Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
      hasErrors = true;
    }
  }

  if (hasErrors) {
    exitInvalidConfig();
  }

  return apiInformation;
};

const assertNoMixedGroups = (options: CliOptions): void => {
  if (options.apiSpecs && exactConfigurationGroup.some(opt => options[opt as keyof CliOptions])) {
    console.error(chalk.red('❌ You cannot use options from multiple configuration groups together.'));
    console.error(chalk.red(`\tGroup 1: ${exactConfigurationGroup.join(', ')}`));
    console.error(chalk.red('\tGroup 2: apiSpecs'));
    exitInvalidConfig();
  }

  const activeGroups = distinctConfigGroups.filter(group => group.some(opt => options[opt as keyof CliOptions]));
  if (activeGroups.length > 1) {
    console.error(chalk.red('❌ You cannot use options from multiple configuration groups together.'));
    activeGroups.forEach((group, idx) => console.error(chalk.red(`\tGroup ${idx + 1}: ${group.join(', ')}`)));
    exitInvalidConfig();
  }
};

const buildExactConfig = (options: CliOptions): Partial<CalculateOptions> => {
  if (exactConfigurationGroup.some(opt => !Object.hasOwn(options, opt))) {
    console.error(chalk.red('❌ Either define a config file or all of these calculation parameters:'));
    exactConfigurationGroup.forEach(opt => console.error(chalk.red(`\t- --${opt.replaceAll(/([A-Z])/g, '-$1').toLowerCase()}`)));
    exitInvalidConfig();
  }

  const { apiName, apiVersion, async, lookbackWindow, qualityGate, serviceName, url } = options;

  return {
    apiInformation: [{ apiName, apiVersion, serviceName }],
    async: async ?? false,
    attributeFilters: parseFilterObjectFromString(options.filter),
    lookbackWindow,
    qualityGate,
    url,
  };
};

const resolveBaseConfig = (options: CliOptions): { base: CliOptions; fileApiSpecs?: string } => {
  // config file explicitly provided
  if (options.configFile) {
    const fileConfig = loadFileConfig(options.configFile);
    return { base: mergeWithCliOverrides(fileConfig, options), fileApiSpecs: fileConfig.apiSpecs };
  }

  // exact parameters provided inline
  if (exactConfigurationGroup.some(opt => options[opt as keyof CliOptions])) {
    return { base: buildExactConfig(options) };
  }

  // apiSpecs-only on CLI, no config file
  if (options.apiSpecs) {
    return { base: mergeWithCliOverrides({}, options) };
  }

  // fall back to default config file discovery
  const fileConfig = loadFileConfig();
  return { base: mergeWithCliOverrides(fileConfig, options), fileApiSpecs: fileConfig.apiSpecs };
};

const applyApiSpecsOverlay = (
  base: CliOptions & Partial<CalculateOptions>,
  options: CliOptions,
  fileApiSpecs?: string,
): Partial<CalculateOptions> => {
  const effectiveApiSpecs = options.apiSpecs ?? fileApiSpecs;

  if (options.apiSpecs && fileApiSpecs && fileApiSpecs !== options.apiSpecs) {
    warnOverride('--api-specs', fileApiSpecs, options.apiSpecs);
  }

  if (!effectiveApiSpecs) {
    return base;
  }

  if (base.apiInformation && base.apiInformation.length > 0) {
    console.warn(chalk.yellow('⚠️ --api-specs is ignored because apiInformation is already defined in the configuration.'));
    return base;
  }

  return {
    ...base,
    apiInformation: loadApiInformationFromGlob(effectiveApiSpecs, base, options),
  };
};

export const validateConfiguration = (config: CalculateOptions): CalculateOptions => {
  if (!config.url) {
    console.error(chalk.red('❌ Snow-White base URL must be defined in the configuration.'));
    exitInvalidConfig();
  }
  if (!config.qualityGate) {
    console.error(chalk.red('❌ Quality-Gate name must be defined in the configuration.'));
    exitInvalidConfig();
  }
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  if (!config.apiInformation || config.apiInformation.length === 0) {
    console.error(chalk.red('❌ At least one API information must be defined in the configuration.'));
    exitInvalidConfig();
  }
  if (config.apiInformation.some(api => !api.serviceName || !api.apiName || !api.apiVersion)) {
    console.error(chalk.red('❌ Each API information must contain serviceName, apiName, and apiVersion.'));
    exitInvalidConfig();
  }
  if (config.lookbackWindow !== undefined && !LOOKBACK_WINDOW_PATTERN.test(config.lookbackWindow)) {
    console.warn(
      chalk.yellow(
        `⚠️ Lookback window "${config.lookbackWindow}" may not be in expected format (e.g., '1h', '24h', '7d'). Proceeding anyway.`,
      ),
    );
  }

  return config;
};

export const sanitizeCalculateOptions = (options: CliOptions): CalculateOptions => {
  assertNoMixedGroups(options);
  const { base, fileApiSpecs } = resolveBaseConfig(options);
  const config = applyApiSpecsOverlay(base, options, fileApiSpecs);
  return validateConfiguration(config as CalculateOptions);
};
