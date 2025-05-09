/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface IOpenApiTestResult {
  openApiCriterionName?: string;
  coverage?: number;
  additionalInformation?: string | null;
}

export const defaultValue: Readonly<IOpenApiTestResult> = {};
