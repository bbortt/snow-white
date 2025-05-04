/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

export interface IOpenApiCriterion {
  name?: string;
  label?: string;
  description?: string | null;
  qualityGateConfigs?: IQualityGateConfig[] | null;
}

export const defaultValue: Readonly<IOpenApiCriterion> = {};
