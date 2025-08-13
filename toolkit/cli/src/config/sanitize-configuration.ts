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

const exactConfigurationGroup = Object.freeze(['serviceName', 'apiName', 'apiVersion']);
const distinctConfigGroups = Object.freeze([exactConfigurationGroup, ['configFile'], ['openApiSpecs']]);

const exitWithCodeInvalidConfig = (): void => {
  exit(INVALID_CONFIG_FORMAT);
};

const validateConfigurationFromFile = (options: CliOptions): CliOptions => {
  if (Object.keys(options).length === 0 || options.configFile) {
    console.error(chalk.red('❌ Configuration file may not contain recursive references or be empty.'));
    exitWithCodeInvalidConfig();
  }

  return options;
};

const loadConfigBasedOnType = (options: CliOptions): object => {
  const activeGroups = distinctConfigGroups.map(group => group.some(opt => options[opt as keyof CliOptions])).filter(Boolean);

  if (activeGroups.length > 1) {
    console.error(chalk.red('❌ You cannot use options from multiple configuration groups together.'));
    distinctConfigGroups.forEach((group, idx) => {
      console.error(chalk.red(`\tGroup ${idx + 1}: ${group.join(', ')}`));
    });
    exitWithCodeInvalidConfig();
  }

  if (activeGroups.length === 0) {
    return validateConfigurationFromFile(resolveConfig());
  } else if (options.configFile) {
    return validateConfigurationFromFile(resolveConfig(options.configFile));
  }

  if (options.openApiSpecs) {
    // TODO: Load OpenAPI specs and merge them into options
    console.warn(chalk.yellow('⚠️ OpenAPI specs are not yet implemented. Using provided options as is.'));
    exit(0);
  }

  if (exactConfigurationGroup.some(opt => !Object.prototype.hasOwnProperty.call(options, opt))) {
    console.error(chalk.red('❌ Either define a config file or all of these calculation parameters:'));
    exactConfigurationGroup.forEach(opt => console.error(chalk.red(`\t- ${opt}`)));
    exitWithCodeInvalidConfig();
  }

  const { qualityGate, serviceName, apiName, apiVersion, url } = options;
  return { apiInformation: [{ serviceName, apiName, apiVersion }], qualityGate, url };
};

export const validateConfiguration = (config: SanitizedOptions): SanitizedOptions => {
  if (!config.url) {
    console.error(chalk.red('❌ Snow-White base URL must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
  }
  if (!config.qualityGate) {
    console.error(chalk.red('❌ Quality-Gate name must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  } else if (!config.apiInformation || config.apiInformation.length === 0) {
    console.error(chalk.red('❌ At least one API information must be defined in the configuration.'));
    exitWithCodeInvalidConfig();
  } else if (config.apiInformation.some(api => !api.serviceName || !api.apiName || !api.apiVersion)) {
    console.error(chalk.red('❌ Each API information must contain serviceName, apiName, and apiVersion.'));
    exitWithCodeInvalidConfig();
  }

  return config;
};

export const sanitizeConfiguration = (options: CliOptions): SanitizedOptions => {
  const config = loadConfigBasedOnType(options);
  return validateConfiguration(config);
};
