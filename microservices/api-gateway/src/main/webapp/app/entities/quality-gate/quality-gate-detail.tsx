/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IOpenApiTestResult } from 'app/shared/model/open-api-test-result.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import OpenapiTestResultTable from 'app/entities/quality-gate/openapi-test-result-table';
import { ShapePieChart } from 'app/entities/quality-gate/shape-pie-chart';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import React, { useEffect } from 'react';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';

import { getEntity } from './quality-gate.reducer';

export const QualityGateDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const qualityGate = useAppSelector(state => state.snowwhite.qualityGate.entity);

  return (
    <Row>
      <Col>
        <h2 data-cy="qualityGateDetailsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.detail.title">QualityGate</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dd>
            <Row>
              <Col md={6}>
                <dl className="jh-entity-details">
                  <dt>
                    <span id="id">
                      <Translate contentKey="global.field.id">ID</Translate>
                    </span>
                  </dt>
                  <dd>{qualityGate.id}</dd>
                  <dt>
                    <span id="calculationId">
                      <Translate contentKey="snowWhiteApp.qualityGate.calculationId">Calculation Id</Translate>
                    </span>
                  </dt>
                  <dd>{qualityGate.calculationId}</dd>
                  <dt>
                    <span id="status">
                      <Translate contentKey="snowWhiteApp.qualityGate.status">Status</Translate>
                    </span>
                  </dt>
                  <dd>
                    <StatusBadge qualityGate={qualityGate} />
                  </dd>
                  <dt>
                    <span id="createdAt">
                      <Translate contentKey="snowWhiteApp.qualityGate.createdAt">Created At</Translate>
                    </span>
                  </dt>
                  <dd>
                    {qualityGate.createdAt ? <TextFormat value={qualityGate.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}
                  </dd>
                  <dt>
                    <Translate contentKey="snowWhiteApp.qualityGate.calculationRequest">Calculation Request</Translate>
                  </dt>
                  <dd>
                    {qualityGate.calculationRequest ? (
                      <pre
                        className="mb-0 p-3"
                        style={{
                          backgroundColor: '#f8f9fa',
                          border: 'none',
                          fontSize: '0.875rem',
                          maxHeight: '400px',
                          overflow: 'auto',
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                        }}
                      >
                        <code style={{ color: '#495057' }}>{JSON.stringify(qualityGate.calculationRequest)}</code>
                      </pre>
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
                <ShapePieChart openApiTestResults={qualityGate.openApiTestResults} />
              </Col>
              <Col md={3}>
                <h3 className="text-center" data-cy="allResultsHeading">
                  <Translate contentKey="snowWhiteApp.qualityGate.shapes.allResults">All Criteria Status</Translate>
                </h3>
                <ShapePieChart
                  openApiTestResults={(qualityGate.openApiTestResults ?? [])
                    .slice()
                    .filter((openApiTestResult: IOpenApiTestResult) => openApiTestResult.isIncludedInQualityGate)}
                />
              </Col>
            </Row>
          </dd>
          <dt>
            <Translate contentKey="snowWhiteApp.qualityGate.openApiTestResult.title">OpenAPI Test Results</Translate>
          </dt>
          <dd>
            <OpenapiTestResultTable openapiTestResults={qualityGate.openApiTestResults} />
          </dd>
        </dl>
        <Button tag={Link} to="/quality-gate" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <a href={`/api/rest/v1/reports/${qualityGate.calculationId}/junit`}>
          <Button replace color="primary">
            <FontAwesomeIcon icon="file-arrow-down" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="snowWhiteApp.qualityGate.action.junitDownload">JUnit Report Download</Translate>
            </span>
          </Button>
        </a>
      </Col>
    </Row>
  );
};

export default QualityGateDetail;
