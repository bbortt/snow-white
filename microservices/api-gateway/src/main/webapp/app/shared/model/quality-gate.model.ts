/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ICalculationRequestParameters } from 'app/shared/model/calculation-request-parameters.model';
import type { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

import { IApiTest } from 'app/shared/model/api-test.model';

export interface IQualityGate {
  calculationId?: string;
  qualityGateConfig?: IQualityGateConfig;
  apiTests?: IApiTest[];
  status?: keyof typeof ReportStatus;
  createdAt?: string;
  calculationRequest?: ICalculationRequestParameters;
}

export const defaultValue: Readonly<IQualityGate> = {};
