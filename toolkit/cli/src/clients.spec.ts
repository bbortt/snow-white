/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';

import type { CalculateQualityGateRequest } from './clients/quality-gate-api';
import type { ListQualityGateReports200ResponseInnerCalculationRequest } from './clients/report-api';

import {
  CalculateQualityGate202ResponseInterfacesInnerTestResultsInnerFromJSON,
  CalculateQualityGate202ResponseInterfacesInnerTestResultsInnerToJSON,
  CalculateQualityGateRequestToJSON,
} from './clients/quality-gate-api';
import {
  GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFromJSON,
  ListQualityGateReports200ResponseInnerCalculationRequestFromJSON,
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerFromJSON,
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerToJSON,
} from './clients/report-api';

const includeApis = [{ apiName: 'user-api', apiVersion: '1.0.0', serviceName: 'user-service' }];

/**
 * One criterion result as the server serves it: four targets judged, one waived out of the fraction
 * before it was measured, three of the four judged ones covered. The same numbers every read shape
 * below is driven over, so a shape that read them differently would show up as a disagreement rather
 * than as a second plausible fixture.
 */
const criterionResultJson = {
  coverage: 0.75,
  id: 'PATH_COVERAGE',
  isIncludedInQualityGate: true,
  judgedTargetCount: 4,
  waivedTargetCount: 1,
};

/** The same result as it arrived before the counts were recorded: the two keys are simply absent. */
const criterionResultJsonWithoutCounts = {
  coverage: 0.75,
  id: 'PATH_COVERAGE',
  isIncludedInQualityGate: true,
};

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

  /**
   * The counts were widened onto the criterion-result component and onto its findings-carrying fork,
   * so they reach the paginated list read, the single-report read and the `202` the calculation
   * trigger answers. This repo generates its own client from those documents, so the question these
   * cases ask is whether the widening is additive *for that client*: every pre-existing key reads and
   * serialises exactly as it did, the two new ones are carried, and a payload that omits them is
   * still read as the object it was read as before they existed.
   */
  describe('criterion result counts', () => {
    it('should carry both counts on the paginated list read, leaving every other key as it was', () => {
      expect(ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerFromJSON(criterionResultJson)).toEqual({
        additionalInformation: undefined,
        coverage: 0.75,
        id: 'PATH_COVERAGE',
        isIncludedInQualityGate: true,
        judgedTargetCount: 4,
        waivedTargetCount: 1,
      });
    });

    it('should carry both counts on the single-report read beside the findings they describe', () => {
      const testResult = GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFromJSON({
        ...criterionResultJson,
        findings: [
          {
            // Empty, as it is for anything but a covered target: a waived one was never traced.
            evidence: [],
            httpMethod: 'GET',
            httpPath: '/api/v1/users',
            responseCode: '409',
            specPointer: '/paths/~1api~1v1~1users/get/responses/409',
            status: 'WAIVED',
            waiverExpiresOn: '2026-12-31',
            waiverReason: 'The upstream fixture cannot produce a conflict yet.',
          },
        ],
      });

      expect(testResult.judgedTargetCount).toBe(4);
      expect(testResult.waivedTargetCount).toBe(1);

      // The fork's one distinguishing key is untouched by the widening, and the waived target it
      // serves is the one the counts account for.
      expect(testResult.findings).toHaveLength(1);
      expect(testResult.findings![0].status).toBe('WAIVED');
      expect(testResult.findings![0].waiverReason).toBe('The upstream fixture cannot produce a conflict yet.');
      expect(testResult.coverage).toBe(0.75);
      expect(testResult.id).toBe('PATH_COVERAGE');
      expect(testResult.isIncludedInQualityGate).toBe(true);
      expect(testResult.additionalInformation).toBeUndefined();
    });

    it('should carry both counts on the 202 the calculation trigger answers', () => {
      expect(CalculateQualityGate202ResponseInterfacesInnerTestResultsInnerFromJSON(criterionResultJson)).toEqual({
        additionalInformation: undefined,
        coverage: 0.75,
        id: 'PATH_COVERAGE',
        isIncludedInQualityGate: true,
        judgedTargetCount: 4,
        waivedTargetCount: 1,
      });
    });

    it('should read a result scored before the counts existed exactly as it did before they existed', () => {
      const fromTheListRead = ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerFromJSON(
        criterionResultJsonWithoutCounts,
      );
      const fromTheAcceptedRead = CalculateQualityGate202ResponseInterfacesInnerTestResultsInnerFromJSON(criterionResultJsonWithoutCounts);

      // Neither key is invented as a `0`, which would read as "this criterion judged nothing" — the
      // one sentence a result without counts must not be made to say.
      for (const testResult of [fromTheListRead, fromTheAcceptedRead]) {
        expect(testResult).toEqual({
          additionalInformation: undefined,
          coverage: 0.75,
          id: 'PATH_COVERAGE',
          isIncludedInQualityGate: true,
          judgedTargetCount: undefined,
          waivedTargetCount: undefined,
        });
      }
    });

    it('should serialise both counts back, and omit them again for a result that never had them', () => {
      const listReadResult = ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerFromJSON(criterionResultJson);

      expect(ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInnerToJSON(listReadResult)).toEqual(criterionResultJson);
      expect(CalculateQualityGate202ResponseInterfacesInnerTestResultsInnerToJSON({ coverage: 1, id: 'PATH_COVERAGE' })).toEqual({
        additionalInformation: undefined,
        coverage: 1,
        id: 'PATH_COVERAGE',
        isIncludedInQualityGate: undefined,
        judgedTargetCount: undefined,
        waivedTargetCount: undefined,
      });
    });
  });
});
