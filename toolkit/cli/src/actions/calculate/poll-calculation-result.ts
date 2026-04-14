/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { ListQualityGateReports200ResponseInner, ReportApi } from '../../clients/report-api';

import { ListQualityGateReports200ResponseInnerStatusEnum } from '../../clients/report-api';
import { QUALITY_GATE_FAILED } from '../../common/exit-codes';

const POLL_INTERVAL_MS = 2000;

export const pollCalculationResult = async (reportApi: ReportApi, calculationId: string): Promise<void> => {
  console.log(chalk.blue('⏳  Polling for calculation result...'));
  console.log('');

  let report: ListQualityGateReports200ResponseInner;
  do {
    await new Promise<void>(resolve => setTimeout(resolve, POLL_INTERVAL_MS));
    report = await reportApi.getReportByCalculationId({ calculationId });
    console.debug(chalk.gray(`Status: ${report.status}`));
  } while (report.status === ListQualityGateReports200ResponseInnerStatusEnum.InProgress);

  console.log('');

  if (report.status === ListQualityGateReports200ResponseInnerStatusEnum.Passed) {
    console.log(chalk.green('✅ Quality-Gate passed!'));
  } else {
    console.error(chalk.red(`❌ Quality-Gate calculation ${report.status}!`));
    if (report.stackTrace) {
      console.error(chalk.gray(report.stackTrace));
    }
    exit(QUALITY_GATE_FAILED);
  }
};
