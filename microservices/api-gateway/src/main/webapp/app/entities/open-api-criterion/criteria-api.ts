/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { CriteriaApi } from 'app/clients/quality-gate-api';

export const criteriaApi = new CriteriaApi(undefined, SERVER_API_URL);
