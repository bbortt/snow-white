/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IApiTest } from 'app/shared/model/api-test.model';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { CodeHighlightBlock } from 'app/entities/quality-gate/code-highlight-block';
import { ShapePieChart } from 'app/entities/quality-gate/shape-pie-chart';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React from 'react';
import { TextFormat, Translate } from 'react-jhipster';
import { Link } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';

interface QualityGateSummaryProps {
  qualityGate: IQualityGate;
}

export const QualityGateSummary: React.FC<QualityGateSummaryProps> = ({ qualityGate }) => {
  const allResults: IApiTestResult[] = qualityGate.apiTests?.flatMap((apiTest: IApiTest) => apiTest.testResults ?? []) ?? [];

  return (
    <Row>
      <Col md={6}>
        <dl className="jh-entity-details">
          <dt>
            <span id="calculationId">
              <Translate contentKey="snowWhiteApp.qualityGate.calculationId">Calculation Id</Translate>
            </span>
          </dt>
          <dd>{qualityGate.calculationId}</dd>
          <dt>
            <span id="qualityGateConfigName">
              <Translate contentKey="snowWhiteApp.qualityGate.qualityGateConfigName">Quality-Gate</Translate>
            </span>
          </dt>
          <dd>
            <Button tag={Link} to={`/quality-gate-config/${qualityGate.qualityGateConfig?.name}`} color="link" size="sm">
              {qualityGate.qualityGateConfig?.name}
            </Button>
          </dd>
          <dt>
            <span id="status">
              <Translate contentKey="snowWhiteApp.qualityGate.status">Status</Translate>
            </span>
          </dt>
          <dd>
            <StatusBadge status={qualityGate.status || ReportStatus.NOT_STARTED} />
          </dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="snowWhiteApp.qualityGate.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>{qualityGate.createdAt ? <TextFormat value={qualityGate.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <Translate contentKey="snowWhiteApp.qualityGate.calculationRequest">Calculation Request</Translate>
          </dt>
          <dd>
            {qualityGate.calculationRequest ? (
              <CodeHighlightBlock code={JSON.stringify(qualityGate.calculationRequest)} language="json" />
            ) : (
              ''
            )}
          </dd>
        </dl>
      </Col>
      <Col md={3}>
        <h3 className="text-center" data-cy="qualityGateResultsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.shapes.qualityGateResults">Included Criteria Status</Translate>
        </h3>
        <ShapePieChart apiTestResults={allResults.filter((r: IApiTestResult) => r.isIncludedInQualityGate)} />
      </Col>
      <Col md={3}>
        <h3 className="text-center" data-cy="allResultsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.shapes.allResults">All Criteria Status</Translate>
        </h3>
        <ShapePieChart apiTestResults={allResults} />
      </Col>
    </Row>
  );
};

export default QualityGateSummary;
