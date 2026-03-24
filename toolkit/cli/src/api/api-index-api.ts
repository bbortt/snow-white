/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ApiIndexApi, Configuration } from '../clients/api-index-api';

export const getApiIndexApi = (baseUrl: string): ApiIndexApi => new ApiIndexApi(new Configuration({ basePath: baseUrl }));
