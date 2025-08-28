/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTest } from 'app/shared/model/api-test.model';

import ApiTestResultTable from 'app/entities/quality-gate/api-test-result-table';
import React, { useState } from 'react';
import { Translate } from 'react-jhipster';
import { Card, CardBody, CardTitle, Col, Collapse, Row, Table } from 'reactstrap';

interface ApiTestCardProps {
  apiTest: IApiTest;
}

export const ApiTestCard: React.FC<ApiTestCardProps> = ({ apiTest }: ApiTestCardProps) => {
  const [isOpen, setIsOpen] = useState(false);

  const toggle = () => setIsOpen(!isOpen);

  return (
    <Card onClick={toggle}>
      <CardTitle>
        <h4>
          {apiTest.serviceName}: {apiTest.apiName} ({apiTest.apiVersion})
        </h4>
      </CardTitle>
      <CardBody>
        <Collapse isOpen={isOpen}>
          {apiTest.testResults && apiTest.testResults.length > 0 ? (
            <ApiTestResultTable apiTestResults={apiTest.testResults} />
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
