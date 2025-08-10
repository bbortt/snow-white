/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import type { AxiosResponse } from 'axios';
import { AxiosError } from 'axios';
import chalk from 'chalk';

import type { CalculateQualityGate202Response, QualityGateApi } from '../clients/quality-gate-api';
import type { CalculateOptions } from './calculate.options';
import { resolveSnowWhiteConfig } from '../common/config';
import { INVALID_CONFIG_FORMAT, QUALITY_GATE_CALCULATION_FAILED } from '../common/exit-codes';

const sanitizeConfiguration = (options: CalculateOptions): CalculateOptions => {
  if (options.config && (options.qualityGate || options.serviceName || options.apiName || options.apiVersion)) {
    console.error(chalk.red(`❌ You cannot use a config file in combination with these calculation parameters:`));
    console.error(chalk.red(`\t- qualityGate`));
    console.error(chalk.red(`\t- serviceName`));
    console.error(chalk.red(`\t- apiName`));
    console.error(chalk.red(`\t- apiVersion`));
    exit(INVALID_CONFIG_FORMAT);
  } else if (options.config) {
    return resolveSnowWhiteConfig(options.config) as unknown as CalculateOptions;
  } else if (!options.qualityGate || !options.serviceName || !options.apiName || !options.apiVersion) {
    console.error(chalk.red(`❌ Either define a config file or all of these calculation parameters:`));
    console.error(chalk.red(`\t- qualityGate`));
    console.error(chalk.red(`\t- serviceName`));
    console.error(chalk.red(`\t- apiName`));
    console.error(chalk.red(`\t- apiVersion`));
    exit(INVALID_CONFIG_FORMAT);
  } else {
    return options;
  }
};

const calculateQualityGate = async (qualityGateApi: QualityGateApi, options: CalculateOptions): Promise<void> => {
  console.log(chalk.blue('🚀 Starting quality gate calculation...'));
  console.log(chalk.gray(`Gate: ${options.qualityGate}`));
  console.log(chalk.gray(`Service: ${options.serviceName}`));
  console.log(chalk.gray(`API: ${options.apiName}`));
  console.log(chalk.gray(`Version: ${options.apiVersion}`));
  console.log(chalk.gray(`Base URL: ${options.url}`));
  console.log('');

  const calculationRequest = {
    serviceName: options.serviceName,
    apiName: options.apiName,
    apiVersion: options.apiVersion,
  };

  const response: AxiosResponse<CalculateQualityGate202Response> = await qualityGateApi.calculateQualityGate(
    options.qualityGate,
    calculationRequest,
  );

  console.log(chalk.green('✅ Quality gate calculation initiated successfully!'));
  console.log('');

  if (response.headers.location) {
    console.log(`Location: ${response.headers.location}`);
  }

  console.log('');
  console.log(chalk.yellow('💡 Use the returned URL to check the calculation report.'));
};

export const calculate = async (qualityGateApi: QualityGateApi, options: CalculateOptions): Promise<void> => {
  options = sanitizeConfiguration(options);

  try {
    await calculateQualityGate(qualityGateApi, options);
  } catch (error: unknown) {
    console.error(chalk.red('❌ Failed to trigger quality gate calculation!'));
    console.log('');

    if (error instanceof AxiosError && error.response) {
      console.error(chalk.red(`Status: ${error.response.status}`));

      if (error.response.data && Object.prototype.hasOwnProperty.call(error.response.data, 'message')) {
        console.error(chalk.red(`Details: ${(error.response.data as { message: string }).message}`));
      } else {
        console.error(chalk.red(`Error: ${error.response.statusText}`));
      }
    } else if (error instanceof AxiosError && error.request) {
      console.error(chalk.red('No response received from server'));
      console.error(chalk.gray('Check if the service is running and accessible'));
    } else {
      console.error(chalk.red(`Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
    }

    exit(QUALITY_GATE_CALCULATION_FAILED);
  }
};
