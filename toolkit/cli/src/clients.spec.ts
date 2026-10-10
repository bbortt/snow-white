/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';

import type { CalculateQualityGateRequest } from './clients/quality-gate-api';
import type { ListQualityGateReports200ResponseInnerCalculationRequest } from './clients/report-api';

import { CalculateQualityGateRequestToJSON } from './clients/quality-gate-api';
import { ListQualityGateReports200ResponseInnerCalculationRequestFromJSON } from './clients/report-api';

const includeApis = [{ apiName: 'user-api', apiVersion: '1.0.0', serviceName: 'user-service' }];

describe('generated clients', () => {
  describe('calculation request', () => {
    it('should serialize a request that declares no waiver exactly as it did before waivers existed', () => {
      const request: CalculateQualityGateRequest = { includeApis, lookbackWindow: '24h' };

      expect(CalculateQualityGateRequestToJSON(request)).toEqual({
        attributeFilters: undefined,
        includeApis,
        lookbackWindow: '24h',
        waivers: undefined,
      });
    });

    it('should serialize the waivers it was given, with expires as a plain date', () => {
      const request: CalculateQualityGateRequest = {
        includeApis,
        waivers: [
          {
            apiName: 'user-api',
            apiVersion: '*',
            criterion: 'PATH_COVERAGE',
            expires: new Date(2026, 11, 31),
            method: 'GET',
            path: '/users/{id}',
            reason: 'The upstream fixture cannot produce a 404 yet.',
            responseCode: '404',
            serviceName: 'user-service',
          },
        ],
      };

      expect(CalculateQualityGateRequestToJSON(request).waivers).toEqual([
        {
          apiName: 'user-api',
          apiVersion: '*',
          contentType: undefined,
          criterion: 'PATH_COVERAGE',
          expires: '2026-12-31',
          method: 'GET',
          parameterName: undefined,
          path: '/users/{id}',
          reason: 'The upstream fixture cannot produce a 404 yet.',
          responseCode: '404',
          serviceName: 'user-service',
        },
      ]);
    });
  });

  describe('report read', () => {
    it('should read a report that was given no waivers without inventing an empty array', () => {
      const calculationRequest = ListQualityGateReports200ResponseInnerCalculationRequestFromJSON({
        includeApis,
        lookbackWindow: '1h',
      });

      expect(calculationRequest.lookbackWindow).toBe('1h');
      expect(calculationRequest.waivers).toBeUndefined();
    });

    it('should read back the waivers the calculation was told about', () => {
      const calculationRequest: ListQualityGateReports200ResponseInnerCalculationRequest =
        ListQualityGateReports200ResponseInnerCalculationRequestFromJSON({
          includeApis,
          waivers: [
            {
              apiName: 'user-api',
              apiVersion: '1.0.0',
              expires: '2026-12-31',
              method: 'POST',
              parameterName: 'X-Correlation-Id',
              path: '/users',
              reason: 'The header is injected by the gateway, never by a caller.',
              serviceName: 'user-service',
            },
          ],
        });

      expect(calculationRequest.waivers).toHaveLength(1);

      const waiver = calculationRequest.waivers![0];
      expect(waiver.parameterName).toBe('X-Correlation-Id');
      expect(waiver.criterion).toBeUndefined();
      expect(waiver.responseCode).toBeUndefined();
      expect(waiver.contentType).toBeUndefined();
      expect(waiver.expires).toEqual(new Date(2026, 11, 31));
    });
  });
});
