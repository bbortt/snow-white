/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { countActiveFilters, extractQualityGateFilterParams } from './quality-gate.utils';

describe('extractQualityGateFilterParams', () => {
  it('returns empty strings when search is empty', () => {
    expect(extractQualityGateFilterParams('')).toEqual({ serviceName: '', apiName: '', apiVersion: '' });
  });

  it('returns empty strings when no filter params are present', () => {
    expect(extractQualityGateFilterParams('?page=1&sort=createdAt%2Cdesc')).toEqual({
      serviceName: '',
      apiName: '',
      apiVersion: '',
    });
  });

  it('extracts serviceName', () => {
    expect(extractQualityGateFilterParams('?serviceName=my-service')).toEqual({
      serviceName: 'my-service',
      apiName: '',
      apiVersion: '',
    });
  });

  it('extracts apiName', () => {
    expect(extractQualityGateFilterParams('?apiName=my-api')).toEqual({
      serviceName: '',
      apiName: 'my-api',
      apiVersion: '',
    });
  });

  it('extracts apiVersion', () => {
    expect(extractQualityGateFilterParams('?apiVersion=1.0.0')).toEqual({
      serviceName: '',
      apiName: '',
      apiVersion: '1.0.0',
    });
  });

  it('extracts all three filter params at once', () => {
    expect(extractQualityGateFilterParams('?serviceName=svc&apiName=api&apiVersion=2.0.0')).toEqual({
      serviceName: 'svc',
      apiName: 'api',
      apiVersion: '2.0.0',
    });
  });

  it('ignores non-filter params like page and sort', () => {
    expect(extractQualityGateFilterParams('?page=2&sort=createdAt%2Cdesc&serviceName=svc')).toEqual({
      serviceName: 'svc',
      apiName: '',
      apiVersion: '',
    });
  });

  it('handles URL-encoded values', () => {
    expect(extractQualityGateFilterParams('?serviceName=my%20service')).toEqual({
      serviceName: 'my service',
      apiName: '',
      apiVersion: '',
    });
  });
});

describe('countActiveFilters', () => {
  it('returns 0 when all params are empty strings', () => {
    expect(countActiveFilters({ serviceName: '', apiName: '', apiVersion: '' })).toBe(0);
  });

  it('counts a single active filter', () => {
    expect(countActiveFilters({ serviceName: 'svc', apiName: '', apiVersion: '' })).toBe(1);
    expect(countActiveFilters({ serviceName: '', apiName: 'api', apiVersion: '' })).toBe(1);
    expect(countActiveFilters({ serviceName: '', apiName: '', apiVersion: '1.0.0' })).toBe(1);
  });

  it('counts two active filters', () => {
    expect(countActiveFilters({ serviceName: 'svc', apiName: 'api', apiVersion: '' })).toBe(2);
  });

  it('counts three active filters', () => {
    expect(countActiveFilters({ serviceName: 'svc', apiName: 'api', apiVersion: '1.0.0' })).toBe(3);
  });
});
