/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export const isSubset = (
  subset: Record<string, string>,
  superset: Record<string, any>,
): boolean => {
  for (const key in subset) {
    if (!(key in superset) || subset[key] !== superset[key]) {
      return false;
    }
  }

  return true;
};
