/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { GlobalOptions } from './global.options';

export interface CalculateOptions extends GlobalOptions {
  qualityGate: string;
  serviceName: string;
  apiName: string;
  apiVersion: string;
  url: string;
}
