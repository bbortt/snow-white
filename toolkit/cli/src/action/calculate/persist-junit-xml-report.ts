/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { writeFileSync } from 'node:fs';

import type { ReportApi } from '../../clients/report-api';

export const persistJUnitXmlReport = async (reportApi: ReportApi, calculationId: string, junitOutput: string): Promise<void> => {
  const blob = await reportApi.getReportByCalculationIdAsJUnit({ calculationId });
  writeFileSync(junitOutput, await blob.text(), 'utf8');
  console.log(chalk.green(`📄 JUnit XML report written to: ${junitOutput}`));
};
