/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { FetchAPI } from '../clients/report-api';

import { Configuration, ReportApi } from '../clients/report-api';
import { createFetchWithRetry } from './fetch-with-retry.ts';

export const getReportApi = (baseUrl: string): ReportApi =>
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  new ReportApi(new Configuration({ basePath: baseUrl, fetchApi: createFetchWithRetry() as FetchAPI }));
