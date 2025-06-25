/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

export interface IQualityGateConfig {
  name?: string;
  description?: string | null;
  isPredefined?: boolean;
  openApiCriteria?: IOpenApiCriterion[] | null;
}

export const defaultValue: Readonly<IQualityGateConfig> = {
  isPredefined: false,
};
