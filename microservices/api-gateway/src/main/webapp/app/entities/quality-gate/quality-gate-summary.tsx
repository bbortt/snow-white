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
import React, { useId, useState } from 'react';
import { TextFormat, translate, Translate } from 'react-jhipster';
import { Link } from 'react-router';
import { Badge, Button, Col, Collapse, Row } from 'reactstrap';

interface QualityGateSummaryProps {
  qualityGate: IQualityGate;
}

export const QualityGateSummary: React.FC<QualityGateSummaryProps> = ({ qualityGate }) => {
  const allResults: IApiTestResult[] = qualityGate.apiTests?.flatMap((apiTest: IApiTest) => apiTest.testResults ?? []) ?? [];
  const attributeFilters: [string, string][] = Object.entries(qualityGate.calculationRequest?.attributeFilters ?? {});
  // Only the finished report by id carries findings: anywhere else a count would read as zero traces rather than unknown.
  const hasFindings = allResults.some((result: IApiTestResult) => result.findings !== undefined);
  const contributingTraces = new Set(
    allResults.flatMap(result => result.findings ?? []).flatMap(finding => finding.evidence.map(evidence => evidence.traceId)),
  );

  const rawRequestId = useId();
  const [rawRequestOpen, setRawRequestOpen] = useState(false);
  const toggleRawRequest = () => setRawRequestOpen(!rawRequestOpen);

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
            <Button
              color="link"
              size="sm"
              style={{ padding: 0 }}
              tag={Link}
              to={`/quality-gate-config/${qualityGate.qualityGateConfig?.name}`}
            >
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
            <span id="lookbackWindow">
              <Translate contentKey="snowWhiteApp.calculationRequestParameters.lookbackWindow">Lookback Window</Translate>
            </span>
          </dt>
          <dd data-cy="lookbackWindow">
            {qualityGate.calculationRequest?.lookbackWindow ? (
              <code>{qualityGate.calculationRequest.lookbackWindow}</code>
            ) : (
              <span className="text-muted">
                <Translate contentKey="snowWhiteApp.qualityGate.trigger.lookbackWindowDefault">Service default</Translate>
              </span>
            )}
          </dd>
          <dt>
            <span id="attributeFilters">
              <Translate contentKey="snowWhiteApp.calculationRequestParameters.attributeFilters">Attribute Filters</Translate>
            </span>
          </dt>
          <dd data-cy="attributeFilters">
            {attributeFilters.length > 0 ? (
              attributeFilters.map(([key, value]) => (
                <Badge key={key} color="light" className="text-dark border font-monospace text-wrap text-break text-start me-1 mb-1">
                  {key} = {value}
                </Badge>
              ))
            ) : (
              <span className="text-muted">
                <Translate contentKey="snowWhiteApp.qualityGate.trigger.noAttributeFilters">None</Translate>
              </span>
            )}
          </dd>
          <dt>
            <span id="testedAPIs">
              <Translate contentKey="snowWhiteApp.qualityGate.testedAPIs">Tested APIs</Translate>
            </span>
          </dt>
          <dd data-cy="testedAPIs">{qualityGate.apiTests?.length ?? 0}</dd>
          {hasFindings ? (
            <>
              <dt>
                <span id="contributingTraces">
                  <Translate contentKey="snowWhiteApp.qualityGate.contributingTraces">Traces that contributed to the coverage</Translate>
                </span>
              </dt>
              <dd data-cy="contributingTraces">{contributingTraces.size}</dd>
            </>
          ) : null}
        </dl>
      </Col>
      <Col md={6}>
        <h3 className="text-center" data-cy="qualityGateResultsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.shapes.qualityGateResults">Quality-Gate Coverage</Translate>
        </h3>
        <ShapePieChart apiTestResults={allResults.filter((r: IApiTestResult) => r.isIncludedInQualityGate)} />
      </Col>
      {/* Outside the chart's row on purpose: the chart fills its column, so the column must not grow with the raw request. */}
      {qualityGate.calculationRequest ? (
        <Col xs={12}>
          <Button
            color="link"
            size="sm"
            className="p-0"
            onClick={toggleRawRequest}
            aria-expanded={rawRequestOpen}
            aria-controls={rawRequestId}
          >
            {/* translate(), not <Translate>: its shouldComponentUpdate ignores a changed contentKey (react-jhipster 1.1.0). */}
            {translate(
              rawRequestOpen ? 'snowWhiteApp.qualityGate.trigger.hideRawRequest' : 'snowWhiteApp.qualityGate.trigger.showRawRequest',
            )}
          </Button>
          <Collapse isOpen={rawRequestOpen} id={rawRequestId}>
            <CodeHighlightBlock code={JSON.stringify(qualityGate.calculationRequest, null, 2)} language="json" />
          </Collapse>
        </Col>
      ) : null}
    </Row>
  );
};

export default QualityGateSummary;
