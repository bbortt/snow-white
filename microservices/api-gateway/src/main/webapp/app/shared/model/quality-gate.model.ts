/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
  /**
   * The threshold this report was scored against, pinned when the calculation was triggered. It is
   * the report's own value, not `qualityGateConfig.minCoveragePercentage` — the latter is the gate's
   * current definition, which a later edit moves and a deletion removes entirely.
   */
  minCoveragePercentage?: number;
  apiTests?: IApiTest[];
  status?: ReportStatus;
  createdAt?: string;
  calculationRequest?: ICalculationRequestParameters;
}

export const defaultValue: Readonly<IQualityGate> = {};
