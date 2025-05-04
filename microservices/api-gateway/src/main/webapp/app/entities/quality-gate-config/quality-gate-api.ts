/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { QualityGateApi } from 'app/clients/quality-gate-api';

export const qualityGateApi = new QualityGateApi(null, SERVER_API_URL);
