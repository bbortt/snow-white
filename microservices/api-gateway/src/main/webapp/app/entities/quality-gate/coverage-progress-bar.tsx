/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { splitCoverage } from 'app/entities/quality-gate/quality-gate.utils';
import { IApiTestResult } from 'app/shared/model/api-test-result.model';
import React from 'react';
import { Progress } from 'reactstrap';

interface CoverageProgressBarProps {
  apiTestResults: IApiTestResult[];
  minCoveragePercentage?: number;
}

export const CoverageProgressBar: React.FC<CoverageProgressBarProps> = ({ apiTestResults, minCoveragePercentage }) => {
  const { total, covered, waived, uncovered } = splitCoverage(apiTestResults);

  const passedPercentage = Math.round((covered / total) * 100);
  const waivedPercentage = Math.round((waived / total) * 100);
  const failedPercentage = Math.round((uncovered / total) * 100);

  return (
    <div style={{ position: 'relative' }}>
      <Progress multi>
        <Progress bar color="success" value={passedPercentage}>
          {passedPercentage} %
        </Progress>
        {waived > 0 ? (
          <Progress bar color="warning" className="text-dark" value={waivedPercentage} data-cy="waivedShare">
            {waivedPercentage} %
          </Progress>
        ) : null}
        <Progress bar color="danger" value={failedPercentage}>
          {failedPercentage} %
        </Progress>
      </Progress>
      {minCoveragePercentage && (
        <div
          style={{
            position: 'absolute',
            top: 0,
            bottom: 0,
            left: `${minCoveragePercentage}%`,
            width: '2px',
            backgroundColor: '#f2f2f2',
            transform: 'translateX(-50%)',
          }}
        />
      )}
    </div>
  );
};
