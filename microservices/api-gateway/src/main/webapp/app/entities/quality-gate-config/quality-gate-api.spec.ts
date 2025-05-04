/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { qualityGateApi } from 'app/entities/quality-gate-config/quality-gate-api';
import { QualityGateApi } from 'app/clients/quality-gate-api';

jest.mock('app/clients/quality-gate-api', () => ({
  QualityGateApi: jest.fn(),
}));

describe('Quality-Gate API', () => {
  it('should be defined', () => {
    expect(qualityGateApi).toBeDefined();
  });

  it('should be constructed', () => {
    expect(QualityGateApi).toHaveBeenCalledWith(null, SERVER_API_URL);
  });
});
