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
import { calculateApiTestStatus } from 'app/entities/quality-gate/quality-gate.utils';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import React, { ReactElement, useMemo, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Col, Collapse, ListGroupItem, Row, Tooltip } from 'reactstrap';
import { v4 as uuidv4 } from 'uuid';

interface ApiTestCardProps {
  apiTest: IApiTest;
  showOnlyIncluded: boolean;
  minCoveragePercentage?: number;
  qualityGateTimedOut: boolean;
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

export const ApiTestCard: React.FC<ApiTestCardProps> = ({
  apiTest,
  showOnlyIncluded,
  minCoveragePercentage,
  qualityGateTimedOut,
}: ApiTestCardProps) => {
  const containsTestResults = useMemo(() => (apiTest.testResults && apiTest.testResults.length > 0) || false, [apiTest.testResults]);
  const uuid = useMemo(() => uuidv4(), []);
  const tooltipId = `Tooltip-${uuid}`;
  const contentId = `ApiTestContent-${uuid}`;

  const [isOpen, setIsOpen] = useState(false);
  const toggleCard = () => setIsOpen(!isOpen);

  const [tooltipOpen, setTooltipOpen] = useState(false);
  const toggleTooltip = () => setTooltipOpen(!tooltipOpen);

  const visibleTestResults: IApiTestResult[] = useMemo(
    () => (showOnlyIncluded ? (apiTest.testResults ?? []).filter(r => r.isIncludedInQualityGate) : (apiTest.testResults ?? [])),
    [apiTest.testResults, showOnlyIncluded],
  );

  return (
    <ListGroupItem>
      <Row className="align-items-center">
        <Col md={7}>
          <h5 className="mb-0">
            <button
              type="button"
              className="btn btn-link text-reset text-decoration-none p-0 text-start"
              onClick={toggleCard}
              aria-expanded={isOpen}
              aria-controls={contentId}
            >
              <FontAwesomeIcon icon={isOpen ? 'chevron-up' : 'chevron-down'} className="me-2" />
              <i>{apiTest.apiName}</i>{' '}
              <small className="fs-6 text-muted">
                <Translate contentKey="snowWhiteApp.apiTest.apiVersion">Version</Translate>: {apiTest.apiVersion}
              </small>
            </button>
          </h5>
        </Col>
        <Col md={2}>
          <StatusBadge status={calculateApiTestStatus(apiTest, qualityGateTimedOut)} />
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
                <Translate contentKey="snowWhiteApp.apiTestResult.coverage">Coverage</Translate>
              </Tooltip>
            </>
          ) : (
            <></>
          )}
        </Col>
      </Row>
      <Collapse isOpen={isOpen} id={contentId}>
        <div className="pt-3">{renderCardContentConditionally(apiTest, containsTestResults, visibleTestResults)}</div>
      </Collapse>
    </ListGroupItem>
  );
};

export default ApiTestCard;
