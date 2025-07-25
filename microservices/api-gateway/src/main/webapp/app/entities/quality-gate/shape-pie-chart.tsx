import { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import { useMemo } from 'react';
import React from 'react';
import { Pie, PieChart, ResponsiveContainer } from 'recharts';

export interface IGroupedTestResult {
  name: string;
  value: number;
}

export const groupOpenApiTestResults = (testResults: IOpenApiTestResult[]): IGroupedTestResult[] => {
  const groups = testResults.reduce(
    (acc, result) => {
      const groupName = result.coverage === 1.0 ? 'PASSED' : 'FAILED';
      acc[groupName] = (acc[groupName] || 0) + 1;
      return acc;
    },
    {} as Record<string, number>,
  );

  return Object.entries(groups).map(([name, value]) => ({
    name,
    value,
  }));
};

export const groupOpenApiTestResultsWithStats = (testResults: IOpenApiTestResult[]) => {
  const total = testResults.length;
  const passed = testResults.filter(result => result.coverage === 1.0).length;
  const failed = total - passed;

  const groups: IGroupedTestResult[] = [];

  if (passed > 0) {
    groups.push({ name: 'PASSED', value: passed });
  }

  if (failed > 0) {
    groups.push({ name: 'FAILED', value: failed });
  }

  return {
    groups,
    stats: {
      total,
      passed,
      failed,
      passRate: total > 0 ? (passed / total) * 100 : 0,
    },
  };
};

type ShapePieChartProps = {
  openApiTestResults: IOpenApiTestResult[];
};

export const ShapePieChart: React.FC<ShapePieChartProps> = ({ openApiTestResults }: ShapePieChartProps) => {
  const data = useMemo(() => groupOpenApiTestResults(openApiTestResults), [openApiTestResults]);

  return (
    <>
      <ResponsiveContainer>
        <PieChart>
          <Pie data={data} />
        </PieChart>
      </ResponsiveContainer>
    </>
  );
};
