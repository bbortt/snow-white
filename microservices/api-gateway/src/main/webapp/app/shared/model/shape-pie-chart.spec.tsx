/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IGroupedTestResult } from 'app/entities/quality-gate/shape-pie-chart';

import { groupOpenApiTestResults, groupOpenApiTestResultsWithStats } from 'app/entities/quality-gate/shape-pie-chart';

import { IOpenApiTestResult } from './open-api-test-result.model';

describe('OpenAPI Test Result Grouping', () => {
  const createTestResult = (coverage: number, name?: string, additionalInfo?: string): IOpenApiTestResult => ({
    openApiCriterionName: name || 'Test Criterion',
    coverage,
    additionalInformation: additionalInfo || null,
  });

  describe('groupOpenApiTestResults', () => {
    it('should group results with coverage 1.0 as PASSED', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(1.0, 'Test 2'),
        createTestResult(1.0, 'Test 3'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([{ name: 'PASSED', value: 3 }]);
    });

    it('should group results with coverage other than 1.0 as FAILED', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(0.5, 'Test 1'),
        createTestResult(0.8, 'Test 2'),
        createTestResult(0.0, 'Test 3'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([{ name: 'FAILED', value: 3 }]);
    });

    it('should group mixed results correctly', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Passed Test 1'),
        createTestResult(0.5, 'Failed Test 1'),
        createTestResult(1.0, 'Passed Test 2'),
        createTestResult(0.8, 'Failed Test 2'),
        createTestResult(1.0, 'Passed Test 3'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'FAILED', value: 2 });
      expect(result).toContainEqual({ name: 'PASSED', value: 3 });
    });

    it('should handle empty array', () => {
      const testResults: IOpenApiTestResult[] = [];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([]);
    });

    it('should handle results with undefined coverage as FAILED', () => {
      const testResults: IOpenApiTestResult[] = [
        { openApiCriterionName: 'Test 1' }, // coverage is undefined
        createTestResult(1.0, 'Test 2'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'FAILED', value: 1 });
      expect(result).toContainEqual({ name: 'PASSED', value: 1 });
    });

    it('should handle edge case coverage values', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Exactly 1.0'),
        createTestResult(0.9999, 'Almost 1.0'),
        createTestResult(1.0001, 'Slightly over 1.0'),
        createTestResult(0, 'Zero coverage'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'PASSED', value: 1 }); // Only exactly 1.0
      expect(result).toContainEqual({ name: 'FAILED', value: 3 }); // Everything else
    });

    it('should maintain consistent order in results', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0.5, 'Failed first'), createTestResult(1.0, 'Passed second')];

      const result = groupOpenApiTestResults(testResults);

      // The order should be based on first occurrence
      expect(result[0]).toEqual({ name: 'FAILED', value: 1 });
      expect(result[1]).toEqual({ name: 'PASSED', value: 1 });
    });
  });

  describe('groupOpenApiTestResultsWithStats', () => {
    it('should return groups and statistics for mixed results', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(1.0, 'Test 3'),
        createTestResult(0.8, 'Test 4'),
      ];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toHaveLength(2);
      expect(result.groups).toContainEqual({ name: 'PASSED', value: 2 });
      expect(result.groups).toContainEqual({ name: 'FAILED', value: 2 });

      expect(result.stats).toEqual({
        total: 4,
        passed: 2,
        failed: 2,
        passRate: 50,
      });
    });

    it('should return only PASSED group when all tests pass', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(1.0, 'Test 2')];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toEqual([{ name: 'PASSED', value: 2 }]);

      expect(result.stats).toEqual({
        total: 2,
        passed: 2,
        failed: 0,
        passRate: 100,
      });
    });

    it('should return only FAILED group when all tests fail', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0.5, 'Test 1'), createTestResult(0.8, 'Test 2')];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toEqual([{ name: 'FAILED', value: 2 }]);

      expect(result.stats).toEqual({
        total: 2,
        passed: 0,
        failed: 2,
        passRate: 0,
      });
    });

    it('should handle empty array with correct statistics', () => {
      const testResults: IOpenApiTestResult[] = [];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toEqual([]);
      expect(result.stats).toEqual({
        total: 0,
        passed: 0,
        failed: 0,
        passRate: 0, // Avoid division by zero
      });
    });

    it('should calculate pass rate correctly with decimal precision', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(0.8, 'Test 3'),
      ];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.stats.passRate).toBeCloseTo(33.33, 1);
      expect(result.stats.total).toBe(3);
      expect(result.stats.passed).toBe(1);
      expect(result.stats.failed).toBe(2);
    });
  });

  describe('Type safety and interface compliance', () => {
    it('should return correct IGroupedTestResult interface', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      const result: IGroupedTestResult[] = groupOpenApiTestResults(testResults);

      expect(result[0]).toHaveProperty('name');
      expect(result[0]).toHaveProperty('value');
      expect(typeof result[0].name).toBe('string');
      expect(typeof result[0].value).toBe('number');
    });

    it('should handle results with only optional properties', () => {
      const testResults: IOpenApiTestResult[] = [
        {}, // All properties are optional
        { coverage: 1.0 },
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'FAILED', value: 1 });
      expect(result).toContainEqual({ name: 'PASSED', value: 1 });
    });
  });
});
