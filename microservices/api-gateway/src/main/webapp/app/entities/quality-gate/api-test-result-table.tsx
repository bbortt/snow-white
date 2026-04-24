/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities } from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import ApiCriterionInfo from 'app/entities/quality-gate/api-criterion-info';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import { TextWithCode } from 'app/shared/TextWithCode';
import React, { createRef, useEffect, useMemo, useRef } from 'react';
import { translate, Translate } from 'react-jhipster';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import { Table } from 'reactstrap';

interface ApiTestResultTableProps {
  apiTestResults: IApiTestResult[];
}

export const ApiTestResultTable: React.FC<ApiTestResultTableProps> = ({ apiTestResults }: ApiTestResultTableProps) => {
  const dispatch = useAppDispatch();

  const openApiCriterionList: IOpenApiCriterion[] | undefined = useAppSelector(state => state.snowwhite.openApiCriterion.entities);

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());

  useEffect(() => {
    dispatch(getEntities());
  }, []);

  const tableBody = useMemo(() => {
    if (!openApiCriterionList || openApiCriterionList.length === 0) {
      return null;
    }

    return apiTestResults
      .slice()
      .sort((a, b) => a.id!.localeCompare(b.id!))
      .flatMap((apiTestResult: IApiTestResult) => {
        const apiCriterion: IOpenApiCriterion | undefined = openApiCriterionList.find(
          (criterion: IOpenApiCriterion) => criterion.name === apiTestResult.id,
        );

        if (!apiCriterion) {
          return [];
        }

        const key = `entity-${apiTestResult.id}`;
        if (!nodeRefs.current.has(key)) {
          nodeRefs.current.set(key, createRef<HTMLTableRowElement>());
        }
        const nodeRef = nodeRefs.current.get(key)!;

        const nameText = translate(`snowWhiteApp.openApiCriterion.description.${apiCriterion.name}.name`);

        return [
          <CSSTransition key={key} timeout={CSS_TRANSITION_TIMEOUT} classNames="row-fade" nodeRef={nodeRef}>
            <tr ref={nodeRef} data-cy="apiTestResultTable">
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
          </CSSTransition>,
        ];
      });
  }, [openApiCriterionList, apiTestResults]);

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
          <TransitionGroup component="tbody">{tableBody}</TransitionGroup>
        </Table>
      </div>
    </div>
  );
};

export default ApiTestResultTable;
