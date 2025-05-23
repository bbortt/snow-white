/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';
import React from 'react';
import { Translate } from 'react-jhipster';
import { Table } from 'reactstrap';

interface OpenapiTestResultTableProps {
  openapiTestResults: IOpenApiTestResult[];
}

export const OpenapiTestResultTable: React.FC<OpenapiTestResultTableProps> = ({ openapiTestResults }: OpenapiTestResultTableProps) => {
  return (
    <div>
      <div className="table-responsive">
        {openapiTestResults && openapiTestResults.length > 0 ? (
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
              {openapiTestResults
                .slice()
                .sort((a, b) => a.openApiCriterionName!.localeCompare(b.openApiCriterionName!))
                .map((openapiTestResult: IOpenApiTestResult, i: number) => (
                  <tr key={`entity-${i}`} data-cy="openApiTestResultTable">
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

export default OpenapiTestResultTable;
