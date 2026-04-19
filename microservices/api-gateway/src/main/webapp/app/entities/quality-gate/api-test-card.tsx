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
import { CoverageProgressBar } from 'app/entities/quality-gate/coverage-progress-bar';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React, { ReactElement, useMemo, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Card, CardBody, CardTitle, Col, Collapse, Row, Tooltip } from 'reactstrap';
import { v4 as uuidv4 } from 'uuid';

interface ApiTestCardProps {
  apiTest: IApiTest;
  qualityGateStatus: ReportStatus;
  showOnlyIncluded: boolean;
}

const isQualityGateStatusOverride = (qualityGateStatus: ReportStatus): boolean => {
  return [ReportStatus.FINISHED_EXCEPTIONALLY, ReportStatus.TIMED_OUT].includes(qualityGateStatus);
};

const renderStatusBadge = (containsTestResults: boolean, testResultsPassed: boolean): ReactElement => {
  if (!containsTestResults) {
    return <StatusBadge status={ReportStatus.IN_PROGRESS} />;
  } else if (testResultsPassed) {
    return <StatusBadge status={ReportStatus.PASSED} />;
  }

  return <StatusBadge status={ReportStatus.FAILED} />;
};

export const ApiTestCard: React.FC<ApiTestCardProps> = ({ apiTest, qualityGateStatus, showOnlyIncluded }: ApiTestCardProps) => {
  const apiTestResultStatus = useMemo(
    () =>
      !apiTest.testResults
        ?.filter(apiTestResult => apiTestResult.isIncludedInQualityGate)
        .some(apiTestResult => !apiTestResult.coverage || apiTestResult.coverage < 1) || false,
    [apiTest.testResults],
  );
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
              {isQualityGateStatusOverride(qualityGateStatus) ? (
                <StatusBadge status={qualityGateStatus} />
              ) : (
                renderStatusBadge(containsTestResults, apiTestResultStatus)
              )}
            </h4>
          </Col>
          <Col md={3}>
            {containsTestResults ? (
              <>
                <div id={tooltipId}>
                  <CoverageProgressBar
                    apiTestResults={apiTest.testResults!.filter(apiTestResult => apiTestResult.isIncludedInQualityGate)}
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
        <Collapse isOpen={isOpen}>
          {containsTestResults ? (
            <ApiTestResultTable apiTestResults={visibleTestResults} />
          ) : (
            <div className="alert alert-warning">
              <Translate contentKey="snowWhiteApp.apiTestResult.home.notFound">No API Test Results found</Translate>
            </div>
          )}
        </Collapse>
      </CardBody>
    </Card>
  );
};

export default ApiTestCard;
