/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IApiTestResult } from 'app/shared/model/api-test-result.model';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';

export interface IApiTest {
  serviceName?: string;
  apiName?: string;
  apiVersion?: string;
  apiType?: string;
  testResults?: IApiTestResult[];
  status?: ReportStatus;
  stackTrace?: string;
}
