/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';
import { getQualityGateApi } from './quality-gate-api';

describe('quality-gate-api', () => {
  describe('getQualityGateApi', () => {
    it('should return QualityGateApi', () => {
      const baseUrl = 'baseUrl';

      const qualityGateApi = getQualityGateApi(baseUrl);
      expect(qualityGateApi).not.toBeUndefined();
      // @ts-expect-error TS2445: Property basePath is protected and only accessible within class BaseAPI and its subclasses.
      expect(qualityGateApi.basePath).toBe(baseUrl);
    });
  });
});
