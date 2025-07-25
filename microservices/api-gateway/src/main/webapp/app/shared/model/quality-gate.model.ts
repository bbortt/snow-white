/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ICalculationRequestParameters } from 'app/shared/model/calculation-request-parameters.model';
import type { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import type { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import type dayjs from 'dayjs';

export interface IQualityGate {
  calculationId?: string;
  qualityGateConfig?: IQualityGateConfig;
  status?: keyof typeof ReportStatus;
  createdAt?: string;
  calculationRequest?: ICalculationRequestParameters;
  openApiTestResults?: IOpenApiTestResult[];
}

export const defaultValue: Readonly<IQualityGate> = {};
