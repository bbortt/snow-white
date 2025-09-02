/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import React from 'react';

import { ShapePieChart, groupOpenApiTestResults, groupOpenApiTestResultsWithStats, IGroupedTestResult } from './shape-pie-chart';

jest.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="responsive-container">{children}</div>,
  PieChart: ({ children }: { children: React.ReactNode }) => <div data-testid="pie-chart">{children}</div>,
  Pie: ({ data, children }: { data: any[]; children: React.ReactNode }) => (
    <div data-testid="pie" data-pie-data={JSON.stringify(data)}>
      {children}
    </div>
  ),
  Cell: ({ fill }: { fill: string }) => <div data-testid="pie-cell" data-fill={fill} />,
  Tooltip: () => <div data-testid="tooltip" />,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <div data-testid="react-jhipster-translate">{contentKey}</div>,
}));

describe('ShapePieChart', () => {
  const createTestResult = (coverage: number, name?: string): IOpenApiTestResult => ({
    openApiCriterionName: name || 'Test Criterion',
    coverage,
    additionalInformation: null,
    isIncludedInQualityGate: false,
  });

  describe('Component rendering', () => {
    it.each([null, undefined, []])('should render alert when apiTestResults is:', (apiTestResults: null | undefined) => {
      const { container } = render(<ShapePieChart apiTestResults={apiTestResults} />);
      expect(container.tagName).toBe('DIV');
      expect(container.firstChild).toHaveClass('alert');
      expect(container.firstChild).toHaveClass('alert-warning');

      const translation = screen.getByTestId('react-jhipster-translate');
      expect(translation).toBeInTheDocument();
      expect(translation.textContent).toEqual('error.chart.noData');
      expect(translation.closest('.alert')).toHaveClass('alert-warning');
    });

    it('should render chart components when data is provided', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(0.5, 'Test 2')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
      expect(screen.getByTestId('pie-chart')).toBeInTheDocument();
      expect(screen.getByTestId('pie')).toBeInTheDocument();
      expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });
  });

  describe('Data processing and chart content', () => {
    it('should pass correct grouped data to Pie component', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Passed Test 1'),
        createTestResult(1.0, 'Passed Test 2'),
        createTestResult(0.5, 'Failed Test 1'),
      ];

      render(<ShapePieChart apiTestResults={testResults} />);

      const pieElement = screen.getByTestId('pie');
      const pieData = JSON.parse(pieElement.getAttribute('data-pie-data') || '[]');

      expect(pieData).toHaveLength(2);
      expect(pieData).toContainEqual({ name: 'PASSED', value: 2 });
      expect(pieData).toContainEqual({ name: 'FAILED', value: 1 });
    });

    it('should render correct number of cells with proper colors', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Passed Test'), createTestResult(0.8, 'Failed Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(2);

      // Check that cells have the correct colors
      const cellColors = cells.map(cell => cell.getAttribute('data-fill'));
      expect(cellColors).toContain('#245c45'); // PASSED color
      expect(cellColors).toContain('#a91320'); // FAILED color
    });

    it('should render only PASSED cell when all tests pass', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(1.0, 'Test 2')];

      render(<ShapePieChart apiTestResults={testResults} />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(1);
      expect(cells[0]).toHaveAttribute('data-fill', '#245c45'); // PASSED color
    });

    it('should render only FAILED cell when all tests fail', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0.5, 'Test 1'), createTestResult(0.8, 'Test 2')];

      render(<ShapePieChart apiTestResults={testResults} />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(1);
      expect(cells[0]).toHaveAttribute('data-fill', '#a91320'); // FAILED color
    });
  });

  describe('Chart configuration', () => {
    it('should include tooltip component', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });
  });

  describe('Memoization behavior', () => {
    it('should memoize data processing based on apiTestResults', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      const { rerender } = render(<ShapePieChart apiTestResults={testResults} />);

      const initialPieData = JSON.parse(screen.getByTestId('pie').getAttribute('data-pie-data') || '[]');

      // Rerender with same data
      rerender(<ShapePieChart apiTestResults={testResults} />);

      const rerenderedPieData = JSON.parse(screen.getByTestId('pie').getAttribute('data-pie-data') || '[]');

      expect(rerenderedPieData).toEqual(initialPieData);
    });

    it('should update memoized data when apiTestResults change', () => {
      const testResults1: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];
      const testResults2: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(0.5, 'Test 2')];

      const { rerender } = render(<ShapePieChart apiTestResults={testResults1} />);

      let pieData = JSON.parse(screen.getByTestId('pie').getAttribute('data-pie-data') || '[]');
      expect(pieData).toHaveLength(1);

      rerender(<ShapePieChart apiTestResults={testResults2} />);

      pieData = JSON.parse(screen.getByTestId('pie').getAttribute('data-pie-data') || '[]');
      expect(pieData).toHaveLength(2);
    });
  });
});

describe('Utility Functions', () => {
  const createTestResult = (coverage: number, name?: string): IOpenApiTestResult => ({
    openApiCriterionName: name || 'Test Criterion',
    coverage,
    additionalInformation: null,
    isIncludedInQualityGate: false,
  });

  describe('groupOpenApiTestResults', () => {
    it('should group results correctly', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(1.0, 'Test 3'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'PASSED', value: 2 });
      expect(result).toContainEqual({ name: 'FAILED', value: 1 });
    });

    it('should handle empty array', () => {
      const result = groupOpenApiTestResults([]);
      expect(result).toEqual([]);
    });

    it('should handle all passed results', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(1.0, 'Test 2')];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([{ name: 'PASSED', value: 2 }]);
    });

    it('should handle all failed results', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0.5, 'Test 1'), createTestResult(0.8, 'Test 2')];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([{ name: 'FAILED', value: 2 }]);
    });

    it('should treat undefined coverage as failed', () => {
      const testResults: IOpenApiTestResult[] = [
        { openApiCriterionName: 'Test 1' } as IOpenApiTestResult, // coverage is undefined
        createTestResult(1.0, 'Test 2'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'PASSED', value: 1 });
      expect(result).toContainEqual({ name: 'FAILED', value: 1 });
    });
  });

  describe('groupOpenApiTestResultsWithStats', () => {
    it('should return groups and stats for mixed results', () => {
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

    it('should handle empty array', () => {
      const result = groupOpenApiTestResultsWithStats([]);

      expect(result.groups).toEqual([]);
      expect(result.stats).toEqual({
        total: 0,
        passed: 0,
        failed: 0,
        passRate: 0,
      });
    });

    it('should calculate pass rate correctly', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(0.8, 'Test 3'),
      ];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.stats.passRate).toBeCloseTo(33.33, 1);
    });

    it('should only include groups with values > 0', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(1.0, 'Test 2')];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toHaveLength(1);
      expect(result.groups).toContainEqual({ name: 'PASSED', value: 2 });
    });
  });

  describe('Type safety', () => {
    it('should ensure IGroupedTestResult has correct structure', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];
      const result: IGroupedTestResult[] = groupOpenApiTestResults(testResults);

      expect(result[0]).toHaveProperty('name');
      expect(result[0]).toHaveProperty('value');
      expect(typeof result[0].name).toBe('string');
      expect(typeof result[0].value).toBe('number');
    });
  });
});
