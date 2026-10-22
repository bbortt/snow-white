/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

/**
 * Parses filter strings in format "key=value" into a record.
 */
export const parseFilterObjectFromString = (filters?: string[]): Record<string, string> | undefined => {
  if (!filters || filters.length === 0) {
    return undefined;
  }

  const result: Record<string, string> = {};
  for (const filter of filters) {
    const separatorIndex = filter.indexOf('=');
    if (separatorIndex === -1) {
      console.warn(chalk.yellow(`⚠️ Ignoring invalid filter format: "${filter}". Expected format: key=value`));
      continue;
    }

    const key = filter.slice(0, separatorIndex).trim();
    const value = filter.slice(separatorIndex + 1).trim();

    if (!key || !value) {
      console.warn(chalk.yellow(`⚠️ Ignoring invalid filter: "${filter}". Both key and value must be non-empty.`));
      continue;
    }

    result[key] = value;
  }

  return Object.keys(result).length > 0 ? result : undefined;
};
