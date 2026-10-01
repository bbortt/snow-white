/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { writeFileSync } from 'node:fs';

import type { ReportApi } from '../../clients/report-api';

export const persistJsonReport = async (reportApi: ReportApi, calculationId: string, reportOutput: string): Promise<void> => {
  // the raw response body is the artifact - a deserialized model would drop what the generated client does not know
  const apiResponse = await reportApi.getReportByCalculationIdRaw({ calculationId });
  writeFileSync(reportOutput, await apiResponse.raw.text(), 'utf8');
  console.log(chalk.green(`📄 JSON report written to: ${reportOutput}`));
};
