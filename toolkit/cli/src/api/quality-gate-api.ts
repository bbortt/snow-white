/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { FetchAPI } from '../clients/quality-gate-api';

import { Configuration, QualityGateApi } from '../clients/quality-gate-api';
import { createFetchWithRetry } from './fetch-with-retry.ts';

export const getQualityGateApi = (baseUrl: string): QualityGateApi =>
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  new QualityGateApi(new Configuration({ basePath: baseUrl, fetchApi: createFetchWithRetry() as FetchAPI }));
