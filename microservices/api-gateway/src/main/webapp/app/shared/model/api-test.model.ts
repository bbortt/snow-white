/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IApiTestResult } from 'app/shared/model/api-test-result.model';

export interface IApiTest {
  serviceName?: string;
  apiName?: string;
  apiVersion?: string;
  apiType?: string;
  testResults?: IApiTestResult[];
}
