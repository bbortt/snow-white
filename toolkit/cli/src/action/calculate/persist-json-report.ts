/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ReportApi } from '../../clients/report-api';

import { persistReportArtifact } from './persist-report-artifact';

export const persistJsonReport = async (
  reportApi: ReportApi,
  calculationId: string,
  reportOutput: string,
  agentic: boolean,
): Promise<void> => {
  // the raw response body is the artifact - a deserialized model would drop what the generated client does not know
  const apiResponse = await reportApi.getReportByCalculationIdRaw({ calculationId });
  persistReportArtifact(reportOutput, await apiResponse.raw.text(), 'JSON report', agentic);
};
