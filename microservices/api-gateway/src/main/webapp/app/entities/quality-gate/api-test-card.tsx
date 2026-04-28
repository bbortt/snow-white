/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './api-test-card.scss';

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IApiTest } from 'app/shared/model/api-test.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import ApiTestResultTable from 'app/entities/quality-gate/api-test-result-table';
import { CodeHighlightBlock } from 'app/entities/quality-gate/code-highlight-block';
import { CoverageProgressBar } from 'app/entities/quality-gate/coverage-progress-bar';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React, { MouseEventHandler, ReactElement, useMemo, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Card, CardBody, CardTitle, Col, Collapse, Row, Tooltip } from 'reactstrap';
import { v4 as uuidv4 } from 'uuid';

interface ApiTestCardProps {
  apiTest: IApiTest;
  showOnlyIncluded: boolean;
  minCoveragePercentage?: number;
}

const renderCardContentConditionally = (
  apiTest: IApiTest,
  containsTestResults: boolean,
  visibleTestResults: IApiTestResult[],
): ReactElement => {
  if (apiTest.stackTrace) {
    return <CodeHighlightBlock code={apiTest.stackTrace} />;
  } else if (containsTestResults) {
    return <ApiTestResultTable apiTestResults={visibleTestResults} />;
  } else {
    return (
      <div className="alert alert-warning">
        <Translate contentKey="snowWhiteApp.apiTestResult.home.notFound">No API Test Results found</Translate>
      </div>
    );
  }
};

export const ApiTestCard: React.FC<ApiTestCardProps> = ({ apiTest, showOnlyIncluded, minCoveragePercentage }: ApiTestCardProps) => {
  const containsTestResults = useMemo(() => (apiTest.testResults && apiTest.testResults.length > 0) || false, [apiTest.testResults]);
  const tooltipId = useMemo(() => `Tooltip-${uuidv4()}`, []);

  const [isOpen, setIsOpen] = useState(false);
  const toggleCard = () => setIsOpen(!isOpen);

  const [tooltipOpen, setTooltipOpen] = useState(false);
  const toggleTooltip = () => setTooltipOpen(!tooltipOpen);

  const visibleTestResults: IApiTestResult[] = useMemo(
    () => (showOnlyIncluded ? (apiTest.testResults ?? []).filter(r => r.isIncludedInQualityGate) : (apiTest.testResults ?? [])),
    [apiTest.testResults, showOnlyIncluded],
  );

  return (
    <Card>
      <CardTitle onClick={toggleCard}>
        <Row className="align-items-center mouse-hover-pointer">
          <Col md={6}>
            <h4 className="mb-0">
              {apiTest.serviceName}: <i>{apiTest.apiName}</i>{' '}
              <small className="fs-6">
                <Translate contentKey="snowWhiteApp.apiTest.apiVersion">Version</Translate>: {apiTest.apiVersion}
              </small>
            </h4>
          </Col>
          <Col md={2}>
            <h4 className="mb-0">
              <StatusBadge status={apiTest.status || ReportStatus.NOT_STARTED} />
            </h4>
          </Col>
          <Col md={3}>
            {containsTestResults ? (
              <>
                <div id={tooltipId}>
                  <CoverageProgressBar
                    apiTestResults={apiTest.testResults!.filter(apiTestResult => apiTestResult.isIncludedInQualityGate)}
                    minCoveragePercentage={minCoveragePercentage}
                  />
                </div>
                <Tooltip isOpen={tooltipOpen} target={tooltipId} toggle={toggleTooltip}>
                  <Translate contentKey="snowWhiteApp.apiTestResult.coverage">Coverage of included Criteria</Translate>
                </Tooltip>
              </>
            ) : (
              <></>
            )}
          </Col>
          <Col md={1} className="d-flex justify-content-end">
            <FontAwesomeIcon icon={isOpen ? 'chevron-up' : 'chevron-down'} />
          </Col>
        </Row>
      </CardTitle>
      <CardBody>
        <Collapse isOpen={isOpen}>{renderCardContentConditionally(apiTest, containsTestResults, visibleTestResults)}</Collapse>
      </CardBody>
    </Card>
  );
};

export default ApiTestCard;
