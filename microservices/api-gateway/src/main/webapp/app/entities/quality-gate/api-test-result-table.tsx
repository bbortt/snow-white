/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities } from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import ApiCriterionInfo from 'app/entities/quality-gate/api-criterion-info';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import { TextWithCode } from 'app/shared/TextWithCode';
import React, { useEffect, useMemo } from 'react';
import { translate, Translate } from 'react-jhipster';
import { Table } from 'reactstrap';

interface ApiTestResultTableProps {
  apiTestResults: IApiTestResult[];
}

export const ApiTestResultTable: React.FC<ApiTestResultTableProps> = ({ apiTestResults }: ApiTestResultTableProps) => {
  const dispatch = useAppDispatch();

  const openApiCriterionList: IOpenApiCriterion[] | undefined = useAppSelector(state => state.snowwhite.openApiCriterion.entities);

  const getAllEntities = () => {
    dispatch(getEntities());
  };

  const handleSyncList = () => {
    getAllEntities();
  };

  useEffect(() => {
    handleSyncList();
  }, []);

  const tableBody = useMemo(() => {
    if (!openApiCriterionList || openApiCriterionList.length === 0) {
      return <></>;
    }

    return apiTestResults
      .slice()
      .sort((a, b) => a.id!.localeCompare(b.id!))
      .map((apiTestResult: IApiTestResult) => {
        const apiCriterion: IOpenApiCriterion | undefined = openApiCriterionList.find(
          (criterion: IOpenApiCriterion) => criterion.name === apiTestResult.id,
        );

        if (!apiCriterion) {
          return <></>;
        }

        const nameText = translate(`snowWhiteApp.openApiCriterion.description.${apiCriterion.name}.name`);

        return (
          <tr key={`entity-${apiTestResult.id}`} data-cy="apiTestResultTable">
            <td>{nameText}</td>
            <td>
              <ApiCriterionInfo apiCriterion={apiCriterion} />
            </td>
            <td>{apiTestResult.coverage}</td>
            <td>{String(apiTestResult.isIncludedInQualityGate)}</td>
            <td>
              <TextWithCode text={apiTestResult.additionalInformation} />
            </td>
          </tr>
        );
      });
  }, [openApiCriterionList]);

  return (
    <div>
      <div className="table-responsive">
        <Table responsive>
          <thead>
            <tr>
              <th>
                <Translate contentKey="snowWhiteApp.apiTestResult.id">API Criterion</Translate>
              </th>
              <th />
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
          <tbody>{tableBody}</tbody>
        </Table>
      </div>
    </div>
  );
};

export default ApiTestResultTable;
