/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IApiTestResult } from 'app/shared/model/api-test-result.model';
import React from 'react';
import { Progress } from 'reactstrap';

interface CoverageProgressBarProps {
  apiTestResults: IApiTestResult[];
}

export const CoverageProgressBar: React.FC<CoverageProgressBarProps> = ({ apiTestResults }) => {
  const total = apiTestResults.length;
  const passed = apiTestResults.reduce((partialSum, r) => partialSum + (r.coverage ?? 0), 0);
  const failed = total - passed;

  const passedPercentage = Math.round((passed / total) * 100);
  const failedPercentage = Math.round((failed / total) * 100);

  return (
    <Progress multi>
      <Progress bar color="success" value={passedPercentage}>
        {passedPercentage} %
      </Progress>
      <Progress bar color="danger" value={failedPercentage}>
        {failedPercentage} %
      </Progress>
    </Progress>
  );
};
