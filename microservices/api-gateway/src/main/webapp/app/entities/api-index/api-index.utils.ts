/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { ASC } from 'app/shared/util/pagination.constants';

export function getSortIconByFieldName(fieldName: string, currentSort: string, order: string): IconDefinition {
  if (currentSort !== fieldName) return faSort;
  return order === ASC ? faSortUp : faSortDown;
}

export function uniqueSortedVersions(versions: string[]): string[] {
  return [...new Set(versions)].sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}
