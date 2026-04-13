/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { CalculateQualityGateRequest, QualityGateApi } from '../clients/quality-gate-api';
import type { ReportApi } from '../clients/report-api';
import type { ListQualityGateReports200ResponseInner } from '../clients/report-api/models/ListQualityGateReports200ResponseInner';
import type { CalculateOptions } from '../config/sanitized-options';

import { FetchError, ResponseError } from '../clients/quality-gate-api/runtime';
import { ListQualityGateReports200ResponseInnerStatusEnum } from '../clients/report-api/models/ListQualityGateReports200ResponseInner';
import { QUALITY_GATE_CALCULATION_FAILED } from '../common/exit-codes';
import { toDtos } from '../entity/mapper/api-information.mapper';

const POLL_INTERVAL_MS = 2000;

const calculateQualityGates = async (qualityGateApi: QualityGateApi, reportApi: ReportApi, options: CalculateOptions): Promise<void> => {
  console.log(chalk.blue(`🚀  Starting Quality-Gate calculation for ${options.apiInformation.length} API(s)...`));
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
    console.log(chalk.yellow('💡  Use the returned URL to check the calculation report.'));
  }

  if (!options.async) {
    const calculationResponse = await apiResponse.value();
    const calculationId = calculationResponse.calculationId;

    console.log('');
    await pollCalculationResult(reportApi, calculationId);
  }
};

const pollCalculationResult = async (reportApi: ReportApi, calculationId: string): Promise<void> => {
  console.log(chalk.blue('⏳  Polling for calculation result...'));
  console.log('');

  let report: ListQualityGateReports200ResponseInner;
  do {
    await new Promise<void>(resolve => setTimeout(resolve, POLL_INTERVAL_MS));
    report = await reportApi.getReportByCalculationId({ calculationId });
    console.log(chalk.gray(`Status: ${report.status}`));
  } while (report.status === ListQualityGateReports200ResponseInnerStatusEnum.InProgress);

  console.log('');

  if (report.status === ListQualityGateReports200ResponseInnerStatusEnum.Passed) {
    console.log(chalk.green('✅ Quality-Gate passed!'));
  } else {
    console.error(chalk.red(`❌  Quality-Gate calculation ${report.status}!`));
    if (report.stackTrace) {
      console.error(chalk.gray(report.stackTrace));
    }
    exit(QUALITY_GATE_CALCULATION_FAILED);
  }
};

export const calculate = async (qualityGateApi: QualityGateApi, reportApi: ReportApi, options: CalculateOptions): Promise<void> => {
  try {
    await calculateQualityGates(qualityGateApi, reportApi, options);
  } catch (error: unknown) {
    console.error(chalk.red('❌  Failed to trigger Quality-Gate calculation!'));
    console.log('');

    if (error instanceof ResponseError) {
      console.error(chalk.red(`Status: ${error.response.status}`));

      const body = (await error.response.json().catch(() => null)) as { message?: string } | null;
      if (body && Object.hasOwn(body, 'message')) {
        console.error(chalk.red(`Details: ${body.message}`));
      } else {
        console.error(chalk.red(`Error: ${error.response.statusText}`));
      }
    } else if (error instanceof FetchError) {
      console.error(chalk.red('No response received from server'));
      console.error(chalk.gray('Check if the service is running and accessible'));
    } else {
      console.error(chalk.red(`Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
    }

    exit(QUALITY_GATE_CALCULATION_FAILED);
  }
};
