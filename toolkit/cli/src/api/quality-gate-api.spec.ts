/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
import { beforeEach, describe, expect, it, mock } from 'bun:test';
import { getApiClient } from './quality-gate-api';

const QualityGateApi = mock().mockReturnValueOnce({});
mock.module('../clients/quality-gate-api', () => ({
  QualityGateApi,
}));

describe('quality-gate-api', () => {
  beforeEach(() => {
    QualityGateApi.mockClear();
  });

  describe('getApiClient', () => {
    it('should return QualityGateApi', () => {
      const baseUrl = 'baseUrl';

      expect(getApiClient(baseUrl)).not.toBeUndefined();

      expect(QualityGateApi).toHaveBeenCalledWith(undefined, baseUrl);
    });
  });
});
