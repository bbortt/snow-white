/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

import type { CalculateQualityGateRequest, QualityGateApi } from '../../clients/quality-gate-api';
import type { ReportApi } from '../../clients/report-api';
import type { CalculateOptions } from '../../config/sanitized-options';

import { toDtos } from '../../entity/mapper/api-information.mapper';
import { pollCalculationResult } from './poll-calculation-result';

export const calculateQualityGates = async (
  qualityGateApi: QualityGateApi,
  reportApi: ReportApi,
  options: CalculateOptions,
): Promise<void> => {
  console.log(chalk.blue(`🚀 Starting Quality-Gate calculation for ${options.apiInformation.length} API(s)...`));
  console.log(chalk.gray(`Base URL: ${options.url}`));

  if (options.lookbackWindow) {
    console.log(chalk.gray(`Lookback window: ${options.lookbackWindow}`));
  }

  if (options.attributeFilters && Object.keys(options.attributeFilters).length > 0) {
    console.log(chalk.gray(`Attribute filters: ${JSON.stringify(options.attributeFilters)}`));
  }

  console.log('');

  const calculationRequest: CalculateQualityGateRequest = {
    attributeFilters: options.attributeFilters,
    includeApis: toDtos(options.apiInformation),
    lookbackWindow: options.lookbackWindow,
  };

  const apiResponse = await qualityGateApi.calculateQualityGateRaw({
    calculateQualityGateRequest: calculationRequest,
    qualityGateConfigName: options.qualityGate,
  });

  console.log(chalk.green('✅ Quality-Gate calculation initiated successfully!'));
  console.log('');

  const location = apiResponse.raw.headers.get('location');
  if (location) {
    console.log(`Location: ${location}`);
    console.log('');
    console.log(chalk.yellow('💡 Use the returned URL to check the calculation report.'));
  }

  if (!options.async) {
    const calculationResponse = await apiResponse.value();
    const calculationId = calculationResponse.calculationId;

    console.log('');
    await pollCalculationResult(reportApi, calculationId);
  }
};
