/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities } from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import ApiCriterionInfo from 'app/entities/quality-gate/api-criterion-info';
import ApiTestFindings from 'app/entities/quality-gate/api-test-findings';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import { TextWithCode } from 'app/shared/TextWithCode';
import React, { createRef, useEffect, useId, useMemo, useRef, useState } from 'react';
import { translate, Translate } from 'react-jhipster';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import { Button, Progress, Table, UncontrolledTooltip } from 'reactstrap';

interface ApiTestResultTableProps {
  apiTestResults: IApiTestResult[];
}

export const ApiTestResultTable: React.FC<ApiTestResultTableProps> = ({ apiTestResults }: ApiTestResultTableProps) => {
  const dispatch = useAppDispatch();

  const openApiCriterionList: IOpenApiCriterion[] | undefined = useAppSelector(state => state.snowwhite.openApiCriterion.entities);

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());
  const nodeRef = (key: string): React.RefObject<HTMLTableRowElement | null> => {
    if (!nodeRefs.current.has(key)) {
      nodeRefs.current.set(key, createRef<HTMLTableRowElement>());
    }
    return nodeRefs.current.get(key)!;
  };

  const tableId = useId();
  const [openFindings, setOpenFindings] = useState<string[]>([]);
  const toggleFindings = (apiTestResultId: string) =>
    setOpenFindings(open => (open.includes(apiTestResultId) ? open.filter(id => id !== apiTestResultId) : [...open, apiTestResultId]));

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
        const rowRef = nodeRef(key);
        const findingsKey = `findings-${apiTestResult.id}`;
        const findingsRef = nodeRef(findingsKey);
        const findingsId = `${tableId}-${findingsKey}`;
        const findings = apiTestResult.findings ?? [];
        const hasFindings = findings.length > 0;
        const findingsOpen = hasFindings && openFindings.includes(apiTestResult.id!);

        const nameText = translate(`snowWhiteApp.openApiCriterion.description.${apiCriterion.name}.name`);

        const passedPercentage = apiTestResult.coverage ? Math.round(apiTestResult.coverage * 100) : 0;
        const failedPercentage = 100 - passedPercentage;

        const includedLabel = translate(
          apiTestResult.isIncludedInQualityGate ? 'snowWhiteApp.apiTestResult.included' : 'snowWhiteApp.apiTestResult.notIncluded',
        );
        const includedTargetId = `included-${apiTestResult.id}`;

        return [
          <CSSTransition key={key} timeout={CSS_TRANSITION_TIMEOUT} classNames="row-fade" nodeRef={rowRef}>
            <tr ref={rowRef} data-cy="apiTestResultTable">
              <td>{nameText}</td>
              <td className="text-center">
                <ApiCriterionInfo apiCriterion={apiCriterion} />
              </td>
              <td>
                <div className="d-flex align-items-center gap-2">
                  {hasFindings ? (
                    <Button
                      color="link"
                      size="sm"
                      className="p-0"
                      onClick={() => toggleFindings(apiTestResult.id!)}
                      aria-expanded={findingsOpen}
                      aria-controls={findingsId}
                      aria-label={translate(
                        findingsOpen ? 'snowWhiteApp.apiTestResult.findings.hide' : 'snowWhiteApp.apiTestResult.findings.show',
                        { criterion: nameText },
                      )}
                    >
                      <FontAwesomeIcon icon="search" aria-hidden="true" />
                    </Button>
                  ) : null}
                  <Progress multi className="flex-grow-1">
                    <Progress bar color="success" value={passedPercentage}>
                      {passedPercentage} %
                    </Progress>
                    <Progress bar color="danger" value={failedPercentage}>
                      {failedPercentage} %
                    </Progress>
                  </Progress>
                </div>
              </td>
              <td className="text-center">
                <span id={includedTargetId} aria-label={includedLabel}>
                  <FontAwesomeIcon
                    icon={apiTestResult.isIncludedInQualityGate ? 'check-circle' : 'times-circle'}
                    className={apiTestResult.isIncludedInQualityGate ? 'text-success' : 'text-muted'}
                    aria-hidden="true"
                  />
                </span>
                <UncontrolledTooltip target={includedTargetId}>{includedLabel}</UncontrolledTooltip>
              </td>
              <td>
                <TextWithCode text={apiTestResult.additionalInformation} />
              </td>
            </tr>
          </CSSTransition>,
          ...(findingsOpen
            ? [
                <CSSTransition key={findingsKey} timeout={CSS_TRANSITION_TIMEOUT} classNames="row-fade" nodeRef={findingsRef}>
                  <tr ref={findingsRef} id={findingsId} data-cy="apiTestFindingsRow">
                    <td colSpan={5}>
                      <ApiTestFindings findings={findings} />
                    </td>
                  </tr>
                </CSSTransition>,
              ]
            : []),
        ];
      });
  }, [openApiCriterionList, apiTestResults, openFindings]);

  return (
    <div>
      <div className="table-responsive">
        <Table responsive style={{ tableLayout: 'fixed' }}>
          <thead>
            <tr>
              <th className="col-md-2">
                <Translate contentKey="snowWhiteApp.apiTestResult.id">API Criterion</Translate>
              </th>
              <th className="col-md-1 text-center" />
              <th className="col-md-2">
                <Translate contentKey="snowWhiteApp.apiTestResult.coverage">Coverage</Translate>
              </th>
              <th className="col-md-1 text-center">
                <Translate contentKey="snowWhiteApp.apiTestResult.isIncludedInQualityGate">Included?</Translate>
              </th>
              <th className="col-md-3">
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
