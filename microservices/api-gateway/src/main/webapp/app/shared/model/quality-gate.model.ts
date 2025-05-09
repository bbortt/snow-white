/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import dayjs from 'dayjs';
import { ICalculationRequestParameters } from 'app/shared/model/calculation-request-parameters.model';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

export interface IQualityGate {
  calculationId?: string;
  qualityGateConfig?: IQualityGateConfig;
  status?: keyof typeof ReportStatus;
  createdAt?: dayjs.Dayjs;
  calculationRequest?: ICalculationRequestParameters;
  openApiTestResults?: IOpenApiTestResult[];
}

export const defaultValue: Readonly<IQualityGate> = {};
