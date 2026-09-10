/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';

import { getSortIconByFieldName, uniqueSortedVersions } from './api-index.utils';

describe('getSortIconByFieldName', () => {
  it('returns faSort when the field is not the current sort field', () => {
    expect(getSortIconByFieldName('apiName', 'otelServiceName', 'asc')).toBe(faSort);
  });

  it('returns faSortUp when the field matches and order is ascending', () => {
    expect(getSortIconByFieldName('apiName', 'apiName', 'asc')).toBe(faSortUp);
  });

  it('returns faSortDown when the field matches and order is descending', () => {
    expect(getSortIconByFieldName('apiName', 'apiName', 'desc')).toBe(faSortDown);
  });
});

describe('uniqueSortedVersions', () => {
  it('returns an empty array for empty input', () => {
    expect(uniqueSortedVersions([])).toEqual([]);
  });

  it('deduplicates identical versions', () => {
    expect(uniqueSortedVersions(['1.0.0', '1.0.0', '2.0.0'])).toEqual(['1.0.0', '2.0.0']);
  });

  it('sorts numerically so v10 comes after v9', () => {
    expect(uniqueSortedVersions(['10.0.0', '2.0.0', '1.0.0', '9.0.0'])).toEqual(['1.0.0', '2.0.0', '9.0.0', '10.0.0']);
  });

  it('handles numeric segments in prefixed strings', () => {
    expect(uniqueSortedVersions(['v10', 'v2', 'v1'])).toEqual(['v1', 'v2', 'v10']);
  });

  it('preserves insertion order for truly equal strings after dedup', () => {
    expect(uniqueSortedVersions(['1.0.0'])).toEqual(['1.0.0']);
  });
});
