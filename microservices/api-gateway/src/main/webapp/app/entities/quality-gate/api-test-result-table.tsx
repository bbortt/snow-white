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
        <Table responsive>
          <thead>
            <tr>
              <th>
                <Translate contentKey="snowWhiteApp.apiTestResult.id">API Criterion</Translate>
              </th>
              <th>
                <Translate contentKey="snowWhiteApp.apiTestResult.coverage">Coverage</Translate>
              </th>
              <th>
                <Translate contentKey="snowWhiteApp.apiTestResult.isIncludedInQualityGate">Included in Quality-Gate?</Translate>
              </th>
              <th>
                <Translate contentKey="snowWhiteApp.apiTestResult.additionalInformation">Additional Information</Translate>
              </th>
            </tr>
          </thead>
          <tbody>
            {apiTestResults
              .slice()
              .sort((a, b) => a.id!.localeCompare(b.id!))
              .map((openapiTestResult: IApiTestResult) => (
                <tr key={`entity-${openapiTestResult.id}`} data-cy="apiTestResultTable">
                  <td>{openapiTestResult.id}</td>
                  <td>{openapiTestResult.coverage}</td>
                  <td>{String(openapiTestResult.isIncludedInQualityGate)}</td>
                  <td>{openapiTestResult.additionalInformation}</td>
                </tr>
              ))}
          </tbody>
        </Table>
      </div>
    </div>
  );
};

export default ApiTestResultTable;
