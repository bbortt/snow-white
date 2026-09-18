/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
  Pie: ({ data, label, children }: { data: any[]; label: (props: { percent?: number }) => React.ReactNode; children: React.ReactNode }) => (
    <div data-testid="pie" data-pie-data={JSON.stringify(data)}>
      <div data-testid="pie-label">{label({ percent: 0.4567 })}</div>
      <div data-testid="pie-label-no-percent">{label({ percent: undefined })}</div>
      {children}
    </div>
  ),
  Cell: ({ fill }: { fill: string }) => <div data-testid="pie-cell" data-fill={fill} />,
  Legend: ({ formatter }: { formatter: (value: string) => React.ReactNode }) => <div data-testid="legend">{formatter('PASSED')}</div>,
  Tooltip: () => <div data-testid="tooltip" />,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <div data-testid="react-jhipster-translate">{contentKey}</div>,
  translate: (key: string) => key,
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
      expect(screen.getByTestId('legend')).toBeInTheDocument();
      expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });
  });

  describe('Data processing and chart content', () => {
    it('should pass correct grouped data to Pie component', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Passed Test 1'),
        createTestResult(1.0, 'Passed Test 2'),
        createTestResult(0.5, 'Partial Test 1'),
      ];

      render(<ShapePieChart apiTestResults={testResults} />);

      const pieElement = screen.getByTestId('pie');
      const pieData = JSON.parse(pieElement.getAttribute('data-pie-data') || '[]');

      expect(pieData).toHaveLength(2);
      expect(pieData).toContainEqual({ name: 'PASSED', value: 2.5 });
      expect(pieData).toContainEqual({ name: 'FAILED', value: 0.5 });
    });

    it('should render correct number of cells with proper colors', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Passed Test'), createTestResult(0.8, 'Partial Test')];

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

    it('should render only FAILED cell when all tests have zero coverage', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0, 'Test 1'), createTestResult(0, 'Test 2')];

      render(<ShapePieChart apiTestResults={testResults} />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(1);
      expect(cells[0]).toHaveAttribute('data-fill', '#a91320'); // FAILED color
    });

    it('should render both cells when coverage is only partial across all results', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0.5, 'Test 1'), createTestResult(0.8, 'Test 2')];

      render(<ShapePieChart apiTestResults={testResults} />);

      const cells = screen.getAllByTestId('pie-cell');
      expect(cells).toHaveLength(2);
    });
  });

  describe('Chart configuration', () => {
    it('should include tooltip component', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });

    it('should render the rounded percentage label', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('pie-label')).toHaveTextContent('46%');
    });

    it('should treat a missing percent as 0% in the label', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('pie-label-no-percent')).toHaveTextContent('0%');
    });

    it('should translate the legend entry name', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test')];

      render(<ShapePieChart apiTestResults={testResults} />);

      expect(screen.getByTestId('legend')).toHaveTextContent('snowWhiteApp.reportStatus.PASSED');
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
    it('should sum fractional coverage rather than bucketing by a pass/fail threshold', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(1.0, 'Test 3'),
      ];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toHaveLength(2);
      expect(result).toContainEqual({ name: 'PASSED', value: 2.5 });
      expect(result).toContainEqual({ name: 'FAILED', value: 0.5 });
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

    it('should handle all zero-coverage results', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(0, 'Test 1'), createTestResult(0, 'Test 2')];

      const result = groupOpenApiTestResults(testResults);

      expect(result).toEqual([{ name: 'FAILED', value: 2 }]);
    });

    it('should treat undefined coverage as zero', () => {
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
      expect(result.groups.find(g => String(g.name) === 'PASSED')?.value).toBeCloseTo(3.3, 5);
      expect(result.groups.find(g => String(g.name) === 'FAILED')?.value).toBeCloseTo(0.7, 5);

      expect(result.stats.total).toBe(4);
      expect(result.stats.covered).toBeCloseTo(3.3, 5);
      expect(result.stats.uncovered).toBeCloseTo(0.7, 5);
      expect(result.stats.coveragePercentage).toBeCloseTo(82.5, 5);
    });

    it('should handle empty array', () => {
      const result = groupOpenApiTestResultsWithStats([]);

      expect(result.groups).toEqual([]);
      expect(result.stats).toEqual({
        total: 0,
        covered: 0,
        uncovered: 0,
        coveragePercentage: 0,
      });
    });

    it('should calculate coverage percentage correctly', () => {
      const testResults: IOpenApiTestResult[] = [
        createTestResult(1.0, 'Test 1'),
        createTestResult(0.5, 'Test 2'),
        createTestResult(0.8, 'Test 3'),
      ];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.stats.coveragePercentage).toBeCloseTo(76.67, 1);
    });

    it('should only include groups with values > 0', () => {
      const testResults: IOpenApiTestResult[] = [createTestResult(1.0, 'Test 1'), createTestResult(1.0, 'Test 2')];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.groups).toHaveLength(1);
      expect(result.groups).toContainEqual({ name: 'PASSED', value: 2 });
    });

    it('should treat undefined coverage as zero when summing covered results', () => {
      const testResults: IOpenApiTestResult[] = [{ openApiCriterionName: 'Test 1' } as IOpenApiTestResult, createTestResult(1.0, 'Test 2')];

      const result = groupOpenApiTestResultsWithStats(testResults);

      expect(result.stats.covered).toBe(1);
      expect(result.stats.uncovered).toBe(1);
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
