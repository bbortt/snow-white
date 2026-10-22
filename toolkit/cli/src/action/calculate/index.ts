/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { QualityGateApi } from '../../clients/quality-gate-api';
import type { ReportApi } from '../../clients/report-api';
import type { CalculateOptions } from '../../config/sanitized-options';

import { isFetchError, isResponseError } from '../../common/error-response-utils';
import { QUALITY_GATE_CALCULATION_FAILED } from '../../common/exit-codes';
import { calculateQualityGates } from './calculate-quality-gates';
import { logResponseError } from './log-response-error';

export const calculate = async (qualityGateApi: QualityGateApi, reportApi: ReportApi, options: CalculateOptions): Promise<void> => {
  try {
    await calculateQualityGates(qualityGateApi, reportApi, options);
  } catch (error: unknown) {
    console.error(chalk.red('❌ Failed to trigger Quality-Gate calculation!'));
    console.log('');

    if (isResponseError(error)) {
      await logResponseError(error);
    } else if (isFetchError(error)) {
      console.error(chalk.red('\t  No response received from server'));
      console.error(chalk.gray('\t  Check if the service is running and accessible'));
    } else {
      console.error(chalk.red(`\t  Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
    }

    exit(QUALITY_GATE_CALCULATION_FAILED);
  }
};
