/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';

import {
  calculateApiTestStatus,
  countActiveFilters,
  extractQualityGateFilterParams,
  groupByService,
  mostSevereStatus,
} from './quality-gate.utils';

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

describe('groupByService', () => {
  it('returns no groups when there are no API tests', () => {
    expect(groupByService([])).toEqual([]);
  });

  it('groups API tests of the same service together, sorted by service, api and version', () => {
    const groups = groupByService([
      { serviceName: 'order-service', apiName: 'payments-api', apiVersion: '2.0.0' },
      { serviceName: 'inventory-service', apiName: 'stock-api', apiVersion: '1.0.0' },
      { serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.1.0' },
      { serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0' },
    ]);

    expect(groups.map(group => group.serviceName)).toEqual(['inventory-service', 'order-service']);
    expect(groups[1].apiTests.map(apiTest => `${apiTest.apiName}@${apiTest.apiVersion}`)).toEqual([
      'orders-api@1.0.0',
      'orders-api@1.1.0',
      'payments-api@2.0.0',
    ]);
  });

  it('does not mutate the given array', () => {
    const apiTests = [{ serviceName: 'b' }, { serviceName: 'a' }];

    groupByService(apiTests);

    expect(apiTests.map(apiTest => apiTest.serviceName)).toEqual(['b', 'a']);
  });
});

describe('calculateApiTestStatus', () => {
  it.each([ReportStatus.PASSED, ReportStatus.FAILED, ReportStatus.FINISHED_EXCEPTIONALLY])(
    'keeps the concluded status %s regardless of quality-gate timeout',
    status => {
      expect(calculateApiTestStatus({ status }, true)).toEqual(status);
      expect(calculateApiTestStatus({ status }, false)).toEqual(status);
    },
  );

  it.each([undefined, ReportStatus.IN_PROGRESS])(
    'reports TIMED_OUT for an unconcluded test (%s) once the quality-gate timed out',
    status => {
      expect(calculateApiTestStatus({ status }, true)).toEqual(ReportStatus.TIMED_OUT);
    },
  );

  it.each([undefined, ReportStatus.IN_PROGRESS])('reports NOT_STARTED for an unconcluded test (%s) otherwise', status => {
    expect(calculateApiTestStatus({ status }, false)).toEqual(ReportStatus.NOT_STARTED);
  });
});

describe('mostSevereStatus', () => {
  it('ranks a run error above a failed quality-gate', () => {
    expect(mostSevereStatus([ReportStatus.PASSED, ReportStatus.FAILED, ReportStatus.FINISHED_EXCEPTIONALLY])).toEqual(
      ReportStatus.FINISHED_EXCEPTIONALLY,
    );
  });

  it('ranks a failed quality-gate above a timeout', () => {
    expect(mostSevereStatus([ReportStatus.TIMED_OUT, ReportStatus.FAILED])).toEqual(ReportStatus.FAILED);
  });

  it('ranks a timeout above anything still unsettled', () => {
    expect(mostSevereStatus([ReportStatus.NOT_STARTED, ReportStatus.TIMED_OUT, ReportStatus.IN_PROGRESS])).toEqual(ReportStatus.TIMED_OUT);
  });

  it('ranks an unsettled test above a passed one', () => {
    expect(mostSevereStatus([ReportStatus.PASSED, ReportStatus.NOT_STARTED])).toEqual(ReportStatus.NOT_STARTED);
  });

  it('reports PASSED only when every test passed', () => {
    expect(mostSevereStatus([ReportStatus.PASSED, ReportStatus.PASSED])).toEqual(ReportStatus.PASSED);
  });

  it('falls back to NOT_STARTED without any status', () => {
    expect(mostSevereStatus([])).toEqual(ReportStatus.NOT_STARTED);
  });
});
