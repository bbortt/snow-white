/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ReportApi } from '../../clients/report-api';

import { persistReportArtifact } from './persist-report-artifact';

export const persistJUnitXmlReport = async (
  reportApi: ReportApi,
  calculationId: string,
  junitOutput: string,
  agentic: boolean,
): Promise<void> => {
  const blob = await reportApi.getReportByCalculationIdAsJUnit({ calculationId });
  persistReportArtifact(junitOutput, await blob.text(), 'JUnit XML report', agentic);
};
