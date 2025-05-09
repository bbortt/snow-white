/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ReportApi } from 'app/clients/report-api';
import { reportApi } from 'app/entities/quality-gate/report-api';

jest.mock('app/clients/report-api', () => ({
  ReportApi: jest.fn(),
}));

describe('Report API', () => {
  it('should be defined', () => {
    expect(reportApi).toBeDefined();
  });

  it('should be constructed', () => {
    expect(ReportApi).toHaveBeenCalledWith(null, SERVER_API_URL);
  });
});
