/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ReportApi } from 'app/clients/report-api';

export const reportApi = new ReportApi(null, SERVER_API_URL);
