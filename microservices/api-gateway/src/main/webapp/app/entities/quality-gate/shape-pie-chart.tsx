/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import React, { useMemo } from 'react';
import { Translate, translate } from 'react-jhipster';
import { Alert } from 'reactstrap';
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

enum ResultType {
  PASSED = 'PASSED',
  FAILED = 'FAILED',
}

export interface IGroupedTestResult {
  name: ResultType;
  value: number;
}

// Sums each result's fractional coverage rather than bucketing by a pass/fail threshold, so the
// chart's covered/uncovered split matches the same criteria's aggregated CoverageProgressBar
// percentage instead of a differently-derived number.
export const groupOpenApiTestResults = (testResults: IApiTestResult[]): IGroupedTestResult[] => {
  const total = testResults.length;
  if (total === 0) {
    return [];
  }

  const covered = testResults.reduce((sum, result) => sum + (result.coverage ?? 0), 0);
  const uncovered = total - covered;

  const groups: IGroupedTestResult[] = [];

  if (covered > 0) {
    groups.push({ name: ResultType.PASSED, value: covered });
  }

  if (uncovered > 0) {
    groups.push({ name: ResultType.FAILED, value: uncovered });
  }

  return groups;
};

export const groupOpenApiTestResultsWithStats = (testResults: IApiTestResult[]) => {
  const total = testResults.length;
  const covered = testResults.reduce((sum, result) => sum + (result.coverage ?? 0), 0);
  const uncovered = total - covered;

  return {
    groups: groupOpenApiTestResults(testResults),
    stats: {
      total,
      covered,
      uncovered,
      coveragePercentage: total > 0 ? (covered / total) * 100 : 0,
    },
  };
};

const COLORS: Record<ResultType, string> = {
  // $ruby-dark
  FAILED: '#a91320',
  // $success-green
  PASSED: '#245c45',
};

type ShapePieChartProps = {
  apiTestResults?: IApiTestResult[];
};

export const ShapePieChart: React.FC<ShapePieChartProps> = ({ apiTestResults }: ShapePieChartProps) => {
  const data: IGroupedTestResult[] = useMemo(() => (apiTestResults ? groupOpenApiTestResults(apiTestResults) : []), [apiTestResults]);

  if (!apiTestResults || apiTestResults.length === 0) {
    return (
      <Alert className="mt-5 text-center" color="warning">
        <Translate contentKey="error.chart.noData">No data for chart available.</Translate>
      </Alert>
    );
  }

  // Passed/Failed are encoded by color, so a percentage label and legend keep the chart
  // readable without relying on color alone (e.g. for colorblind users).
  return (
    <ResponsiveContainer>
      <PieChart>
        <Pie
          data={data as unknown as Record<string, unknown>[]}
          innerRadius="50%"
          labelLine={false}
          label={({ percent }: { percent?: number }) => `${Math.round((percent ?? 0) * 100)}%`}
        >
          {data.map((entry: IGroupedTestResult) => (
            <Cell key={`cell-${entry.name}`} fill={COLORS[entry.name]} />
          ))}
        </Pie>
        <Legend formatter={(value: ResultType) => translate(`snowWhiteApp.reportStatus.${value}`)} />
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
};
