/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './api-test-card.scss';

import type { IApiTest } from 'app/shared/model/api-test.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import ApiTestResultTable from 'app/entities/quality-gate/api-test-result-table';
import { IApiTestResult } from 'app/shared/model/api-test-result.model';
import React, { ReactElement, useMemo, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Badge, Card, CardBody, CardTitle, Col, Collapse, Progress, Row } from 'reactstrap';

interface ApiTestCardProps {
  apiTest: IApiTest;
}

const renderStatusBadge = (containsTestResults: boolean, testResultsPassed: boolean): ReactElement => {
  if (!containsTestResults) {
    return (
      <Badge color="info">
        <Translate contentKey="snowWhiteApp.reportStatus.IN_PROGRESS">In Progress</Translate>
      </Badge>
    );
  } else if (testResultsPassed) {
    return (
      <Badge color="success">
        <Translate contentKey="snowWhiteApp.reportStatus.PASSED">Passed</Translate>
      </Badge>
    );
  }

  return (
    <Badge color="danger">
      <Translate contentKey="snowWhiteApp.reportStatus.FAILED">Failed</Translate>
    </Badge>
  );
};

const renderStatusProgressBar = (containsTestResults: boolean, apiTestResults: IApiTestResult[]): ReactElement => {
  if (!containsTestResults) {
    return <></>;
  }

  const total = apiTestResults.length;
  const passed =
    apiTestResults
      .filter(apiTestResult => !apiTestResult.coverage || apiTestResult.coverage < 1)
      .map(apiTestResult => apiTestResult.coverage)
      .reduce((partialSum, a) => (partialSum ?? 0) + (a ?? 0), 0) ?? 0;
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

export const ApiTestCard: React.FC<ApiTestCardProps> = ({ apiTest }: ApiTestCardProps) => {
  const containsTestResults = useMemo(() => (apiTest.testResults && apiTest.testResults.length > 0) || false, [apiTest.testResults]);
  const apiTestResultStatus = useMemo(
    () => !apiTest.testResults?.some(apiTestResult => !apiTestResult.coverage || apiTestResult.coverage < 1) || false,
    [apiTest.testResults],
  );

  const [isOpen, setIsOpen] = useState(false);

  const toggle = () => setIsOpen(!isOpen);

  return (
    <Card>
      <CardTitle onClick={toggle}>
        <Row className="align-items-center mouse-hover-pointer">
          <Col md={6}>
            <h4>
              {apiTest.serviceName}: <i>{apiTest.apiName}</i>{' '}
              <small className="fs-6">
                <Translate contentKey="snowWhiteApp.apiTest.apiVersion">Version</Translate>: {apiTest.apiVersion}
              </small>
            </h4>
          </Col>
          <Col md={2}>
            <h4>{renderStatusBadge(containsTestResults, apiTestResultStatus)}</h4>
          </Col>
          <Col md={1}>
            <Translate contentKey="snowWhiteApp.apiTestResult.coverage">Coverage</Translate>:
          </Col>
          <Col md={2}>{renderStatusProgressBar(containsTestResults, apiTest.testResults!)}</Col>
          <Col md={1}>{isOpen ? <FontAwesomeIcon icon="chevron-down" /> : <FontAwesomeIcon icon="chevron-up" />}</Col>
        </Row>
      </CardTitle>
      <CardBody>
        <Collapse isOpen={isOpen}>
          {containsTestResults ? (
            <ApiTestResultTable apiTestResults={apiTest.testResults!} />
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
