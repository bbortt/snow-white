/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { SanitizedOptions } from './sanitized-options';

export interface CliOptions extends SanitizedOptions {
  config: string;
}
