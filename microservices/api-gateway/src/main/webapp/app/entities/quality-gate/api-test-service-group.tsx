/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTest } from 'app/shared/model/api-test.model';

import { ApiTestCard } from 'app/entities/quality-gate/api-test-card';
import { calculateApiTestStatus, mostSevereStatus } from 'app/entities/quality-gate/quality-gate.utils';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import React, { useId, useMemo } from 'react';
import { Translate } from 'react-jhipster';
import { Card, CardHeader, ListGroup } from 'reactstrap';

interface ApiTestServiceGroupProps {
  serviceName: string;
  apiTests: IApiTest[];
  showOnlyIncluded: boolean;
  minCoveragePercentage?: number;
  qualityGateTimedOut: boolean;
}

export const ApiTestServiceGroup: React.FC<ApiTestServiceGroupProps> = ({
  serviceName,
  apiTests,
  showOnlyIncluded,
  minCoveragePercentage,
  qualityGateTimedOut,
}: ApiTestServiceGroupProps) => {
  const headingId = useId();

  const serviceStatus = useMemo(
    () => mostSevereStatus(apiTests.map(apiTest => calculateApiTestStatus(apiTest, qualityGateTimedOut))),
    [apiTests, qualityGateTimedOut],
  );

  return (
    <Card tag="section" className="mb-3" aria-labelledby={headingId} data-cy="apiTestServiceGroup">
      <CardHeader className="d-flex flex-wrap align-items-center gap-2">
        <h4 id={headingId} className="mb-0 me-auto">
          {serviceName}
        </h4>
        <span className="text-muted">
          <Translate contentKey="snowWhiteApp.apiTest.serviceGroup.apiCount" interpolate={{ count: apiTests.length }}>
            {`APIs: ${apiTests.length}`}
          </Translate>
        </span>
        <StatusBadge status={serviceStatus} />
      </CardHeader>
      <ListGroup flush>
        {apiTests.map((apiTest: IApiTest) => (
          <ApiTestCard
            apiTest={apiTest}
            showOnlyIncluded={showOnlyIncluded}
            minCoveragePercentage={minCoveragePercentage}
            qualityGateTimedOut={qualityGateTimedOut}
            key={`api-test-${apiTest.serviceName}-${apiTest.apiName}-${apiTest.apiVersion}`}
          />
        ))}
      </ListGroup>
    </Card>
  );
};

export default ApiTestServiceGroup;
