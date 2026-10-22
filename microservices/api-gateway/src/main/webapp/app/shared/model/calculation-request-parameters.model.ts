/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

/** One API the calculation was asked to cover. */
export interface IIncludedApi {
  serviceName: string;
  apiName: string;
  apiVersion: string;
}

/** One criterion declared inapplicable to one target of one API, as the calculation was triggered with it. */
export interface IWaiver {
  serviceName: string;
  apiName: string;
  apiVersion: string;
  path: string;
  method: string;
  responseCode?: string;
  parameterName?: string;
  contentType?: string;
  criterion?: string;
  reason: string;
  // The last day the waiver is in force, as a calendar date (YYYY-MM-DD) - not an instant.
  expires: string;
}

export interface ICalculationRequestParameters {
  includeApis?: IIncludedApi[];
  lookbackWindow?: string;
  attributeFilters?: Record<string, string>;
  // Absent rather than empty on a calculation triggered without waivers.
  waivers?: IWaiver[];
}

export const defaultValue: Readonly<ICalculationRequestParameters> = {};
