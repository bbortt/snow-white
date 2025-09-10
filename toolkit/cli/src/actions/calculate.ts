/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import type { AxiosResponse } from 'axios';
import { AxiosError } from 'axios';
import chalk from 'chalk';

import type { CalculateQualityGate202Response, CalculateQualityGateRequest, QualityGateApi } from '../clients/quality-gate-api';
import { QUALITY_GATE_CALCULATION_FAILED } from '../common/exit-codes';
import type { SanitizedOptions } from '../config/sanitized-options';
import { toDtos } from '../entity/mapper/api-information.mapper';

const calculateQualityGates = async (qualityGateApi: QualityGateApi, options: SanitizedOptions): Promise<void> => {
  console.log(chalk.blue(`🚀 Starting Quality-Gate calculation for ${options.apiInformation.length} API(s)...`));
  console.log(chalk.gray(`Base URL: ${options.url}`));
  console.log('');

  const calculationRequest: CalculateQualityGateRequest = { includeApis: toDtos(options.apiInformation) };

  const response: AxiosResponse<CalculateQualityGate202Response> = await qualityGateApi.calculateQualityGate(
    options.qualityGate,
    calculationRequest,
  );

  console.log(chalk.green('✅ Quality-Gate calculation initiated successfully!'));
  console.log('');

  if (response.headers.location) {
    console.log(`Location: ${response.headers.location}`);
    console.log('');
    console.log(chalk.yellow('💡 Use the returned URL to check the calculation report.'));
  }
};

// TODO: https://sonarcloud.io/project/issues?id=bbortt_snow-white&pullRequest=570&issueStatuses=OPEN,CONFIRMED&sinceLeakPeriod=true

export const calculate = async (qualityGateApi: QualityGateApi, options: SanitizedOptions): Promise<void> => {
  try {
    await calculateQualityGates(qualityGateApi, options);
  } catch (error: unknown) {
    console.error(chalk.red('❌ Failed to trigger Quality-Gate calculation!'));
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
