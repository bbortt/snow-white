import { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import { useMemo } from 'react';
import React from 'react';
import { Translate } from 'react-jhipster';
import { Alert } from 'reactstrap';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

enum ResultType {
  PASSED = 'PASSED',
  FAILED = 'FAILED',
}
export interface IGroupedTestResult {
  name: ResultType;
  value: number;
}

export const groupOpenApiTestResults = (testResults: IOpenApiTestResult[]): IGroupedTestResult[] => {
  const groups = testResults.reduce(
    (acc, result) => {
      const groupName: ResultType = result.coverage === 1.0 ? ResultType.PASSED : ResultType.FAILED;
      acc[groupName] = (acc[groupName] || 0) + 1;
      return acc;
    },
    {} as Record<ResultType, number>,
  );

  return Object.entries(groups).map(
    ([name, value]) =>
      ({
        name,
        value,
      }) as IGroupedTestResult,
  );
};

export const groupOpenApiTestResultsWithStats = (testResults: IOpenApiTestResult[]) => {
  const total = testResults.length;
  const passed = testResults.filter(result => result.coverage === 1.0).length;
  const failed = total - passed;

  const groups: IGroupedTestResult[] = [];

  if (passed > 0) {
    groups.push({ name: ResultType.PASSED, value: passed });
  }

  if (failed > 0) {
    groups.push({ name: ResultType.FAILED, value: failed });
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

const COLORS: Record<ResultType, string> = {
  // $ruby-dark
  FAILED: '#a91320',
  // $success-green
  PASSED: '#245c45',
};

type ShapePieChartProps = {
  openApiTestResults?: IOpenApiTestResult[];
};

export const ShapePieChart: React.FC<ShapePieChartProps> = ({ openApiTestResults }: ShapePieChartProps) => {
  const data: IGroupedTestResult[] = useMemo(
    () => (openApiTestResults ? groupOpenApiTestResults(openApiTestResults) : []),
    [openApiTestResults],
  );

  if (!openApiTestResults) {
    return (
      <Alert color="warning">
        <Translate contentKey="error.chart.noData">No data for chart available.</Translate>
      </Alert>
    );
  }

  return (
    <>
      <ResponsiveContainer>
        <PieChart>
          <Pie data={data} innerRadius="50%" labelLine={false}>
            {data.map((entry: IGroupedTestResult) => (
              <Cell key={`cell-${entry.name}`} fill={COLORS[entry.name]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
    </>
  );
};
