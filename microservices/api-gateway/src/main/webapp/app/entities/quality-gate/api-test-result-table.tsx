/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import { TextWithCode } from 'app/shared/TextWithCode';
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
              .map((apiTestResult: IApiTestResult) => (
                <tr key={`entity-${apiTestResult.id}`} data-cy="apiTestResultTable">
                  <td>{apiTestResult.id}</td>
                  <td>{apiTestResult.coverage}</td>
                  <td>{String(apiTestResult.isIncludedInQualityGate)}</td>
                  <td>
                    <TextWithCode text={apiTestResult.additionalInformation} />
                  </td>
                </tr>
              ))}
          </tbody>
        </Table>
      </div>
    </div>
  );
};

export default ApiTestResultTable;
