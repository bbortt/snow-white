/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import { splitCoverage } from 'app/entities/quality-gate/quality-gate.utils';
import React, { useMemo } from 'react';
import { Translate, translate } from 'react-jhipster';
import { Alert } from 'reactstrap';
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

enum ResultType {
  PASSED = 'PASSED',
  WAIVED = 'WAIVED',
  FAILED = 'FAILED',
}

export interface IGroupedTestResult {
  name: ResultType;
  value: number;
}

// Same split as the CoverageProgressBar, so the chart and the bar never disagree.
export const groupOpenApiTestResults = (testResults: IApiTestResult[]): IGroupedTestResult[] => {
  const { covered, waived, uncovered } = splitCoverage(testResults);

  return [
    { name: ResultType.PASSED, value: covered },
    { name: ResultType.WAIVED, value: waived },
    { name: ResultType.FAILED, value: uncovered },
  ].filter(group => group.value > 0);
};

export const groupOpenApiTestResultsWithStats = (testResults: IApiTestResult[]) => {
  const split = splitCoverage(testResults);

  return {
    groups: groupOpenApiTestResults(testResults),
    stats: {
      ...split,
      coveragePercentage: split.total > 0 ? (split.covered / split.total) * 100 : 0,
    },
  };
};

const COLORS: Record<ResultType, string> = {
  // $ruby-dark
  FAILED: '#a91320',
  // $warning-amber
  WAIVED: '#d9861a',
  // $success-green
  PASSED: '#245c45',
} as const;

// Waived is a finding status, not a report status (CON-012 adds none), so its legend label comes
// from the findings vocabulary.
const legendLabel = (value: ResultType): string =>
  translate(value === ResultType.WAIVED ? 'snowWhiteApp.apiTestResult.findings.status.WAIVED' : `snowWhiteApp.reportStatus.${value}`);

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
    <ResponsiveContainer className="pb-4">
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
        <Legend formatter={legendLabel} />
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
};
