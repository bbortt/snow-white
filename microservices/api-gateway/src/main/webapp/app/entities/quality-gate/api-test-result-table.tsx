/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import React from 'react';
import { Translate } from 'react-jhipster';
import { Table } from 'reactstrap';

interface ApiTestResultTableProps {
  apiTestResults: IApiTestResult[];
}

export const ApiTestResultTable: React.FC<ApiTestResultTableProps> = ({ apiTestResults }: ApiTestResultTableProps) => {
  return (
    <div>
      <div className="table-responsive">
        {apiTestResults && apiTestResults.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.openApiTestResult.name">Name</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.openApiTestResult.coverage">Coverage</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.openApiTestResult.additionalInformation">
                    Additional Information
                  </Translate>
                </th>
              </tr>
            </thead>
            <tbody>
              {apiTestResults
                .slice()
                .sort((a, b) => a.openApiCriterionName!.localeCompare(b.openApiCriterionName))
                .map((openapiTestResult: IOpenApiTestResult, i: number) => (
                  <tr key={`entity-${openapiTestResult.openApiCriterionName}`} data-cy="openApiTestResultTable">
                    <td>{openapiTestResult.openApiCriterionName}</td>
                    <td>{openapiTestResult.coverage}</td>
                    <td>{openapiTestResult.additionalInformation}</td>
                  </tr>
                ))}
            </tbody>
          </Table>
        ) : (
          <div className="alert alert-warning">
            <Translate contentKey="snowWhiteApp.qualityGate.home.notFound">No OpenAPI Test Results found</Translate>
          </div>
        )}
      </div>
    </div>
  );
};

export default ApiTestResultTable;
