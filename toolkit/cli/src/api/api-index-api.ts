/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { FetchAPI } from '../clients/api-index-api';

import { ApiIndexApi, Configuration } from '../clients/api-index-api';
import { createFetchWithRetry } from './fetch-with-retry.ts';

export const getApiIndexApi = (baseUrl: string): ApiIndexApi =>
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  new ApiIndexApi(new Configuration({ basePath: baseUrl, fetchApi: createFetchWithRetry() as FetchAPI }));
