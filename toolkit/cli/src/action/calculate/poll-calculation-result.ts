/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';
import { setTimeout as sleep } from 'node:timers/promises';

import type { ListQualityGateReports200ResponseInner, ReportApi } from '../../clients/report-api';

import { ListQualityGateReports200ResponseInnerStatusEnum } from '../../clients/report-api';
import { QUALITY_GATE_FAILED } from '../../common/exit-codes';
import { AgenticQualityGateResponseTransformer } from './agent-quality-gate-response-transformer.ts';

const POLL_INTERVAL_MS = 2000;

export const pollCalculationResult = async (reportApi: ReportApi, calculationId: string, agentic: boolean): Promise<boolean> => {
  // @ts-expect-error TS2341: Property configuration is private and only accessible within class Configuration
  const baseApiPath = reportApi.configuration.configuration.basePath;
  if (!baseApiPath) {
    throw new Error('Invalid Snow-White base API path supplied!');
  }

  return pollCalculationResultWithTransformer(reportApi, calculationId, agentic, new AgenticQualityGateResponseTransformer(baseApiPath));
};

export const pollCalculationResultWithTransformer = async (
  reportApi: ReportApi,
  calculationId: string,
  agentic: boolean,
  transformer: AgenticQualityGateResponseTransformer,
): Promise<boolean> => {
  if (!agentic) {
    console.log(chalk.blue('⏳ Polling for calculation result...'));
    console.log('');
  }

  let report: ListQualityGateReports200ResponseInner;
  do {
    await sleep(POLL_INTERVAL_MS);
    report = await reportApi.getReportByCalculationId({ calculationId });

    if (!agentic) {
      console.debug(chalk.gray(`Status: ${report.status}`));
    }
  } while (report.status === ListQualityGateReports200ResponseInnerStatusEnum.InProgress);

  if (!agentic) {
    console.log('');
  }

  const passed = report.status === ListQualityGateReports200ResponseInnerStatusEnum.Passed;

  if (!agentic) {
    if (passed) {
      console.log(chalk.green('✅ Quality-Gate passed!'));
    } else {
      console.error(chalk.red(`❌ Quality-Gate calculation ${report.status}!`));
    }
  }

  if (agentic) {
    console.info(JSON.stringify(transformer.transform(report)));
    // @ts-expect-error TS2339: Property stackTrace does not exist on type ListQualityGateReports200ResponseInner
  } else if (report.stackTrace) {
    // @ts-expect-error TS2339: Property stackTrace does not exist on type ListQualityGateReports200ResponseInner
    console.error(chalk.gray(report.stackTrace));
  }

  if (report.status === ListQualityGateReports200ResponseInnerStatusEnum.FinishedExceptionally) {
    exit(QUALITY_GATE_FAILED);
  }

  return passed;
};
