/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IApiTest } from 'app/shared/model/api-test.model';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { ApiTestTable } from 'app/entities/quality-gate/api-test-table';
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

  const loading = useAppSelector(state => state.snowwhite.qualityGate.loading);
  const qualityGateEntity: IQualityGate = useAppSelector(state => state.snowwhite.qualityGate.entity);

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
                    <span id="calculationId">
                      <Translate contentKey="snowWhiteApp.qualityGate.calculationId">Calculation Id</Translate>
                    </span>
                  </dt>
                  <dd>{qualityGateEntity.calculationId}</dd>
                  <dt>
                    <span id="qualityGateConfigName">
                      <Translate contentKey="snowWhiteApp.qualityGate.qualityGateConfigName">Quality-Gate</Translate>
                    </span>
                  </dt>
                  <dd>
                    <Button tag={Link} to={`/quality-gate-config/${qualityGateEntity.qualityGateConfig?.name}`} color="link" size="sm">
                      {qualityGateEntity.qualityGateConfig?.name}
                    </Button>
                  </dd>
                  <dt>
                    <span id="status">
                      <Translate contentKey="snowWhiteApp.qualityGate.status">Status</Translate>
                    </span>
                  </dt>
                  <dd>
                    <StatusBadge qualityGate={qualityGateEntity} />
                  </dd>
                  <dt>
                    <span id="createdAt">
                      <Translate contentKey="snowWhiteApp.qualityGate.createdAt">Created At</Translate>
                    </span>
                  </dt>
                  <dd>
                    {qualityGateEntity.createdAt ? (
                      <TextFormat value={qualityGateEntity.createdAt} type="date" format={APP_DATE_FORMAT} />
                    ) : null}
                  </dd>
                  <dt>
                    <Translate contentKey="snowWhiteApp.qualityGate.calculationRequest">Calculation Request</Translate>
                  </dt>
                  <dd>
                    {qualityGateEntity.calculationRequest ? (
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
                        <code style={{ color: '#495057' }}>{JSON.stringify(qualityGateEntity.calculationRequest)}</code>
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
                <ShapePieChart
                  apiTestResults={(qualityGateEntity.apiTests?.flatMap((apiTest: IApiTest) => apiTest.testResults ?? []) ?? [])
                    .slice()
                    .filter((apiTestResult: IApiTestResult) => apiTestResult.isIncludedInQualityGate)}
                />
              </Col>
              <Col md={3}>
                <h3 className="text-center" data-cy="allResultsHeading">
                  <Translate contentKey="snowWhiteApp.qualityGate.shapes.allResults">All Criteria Status</Translate>
                </h3>
                <ShapePieChart
                  apiTestResults={qualityGateEntity.apiTests?.flatMap((apiTest: IApiTest) => apiTest.testResults ?? []) ?? []}
                />
              </Col>
            </Row>
          </dd>
          <dt>
            <Translate contentKey="snowWhiteApp.qualityGate.openApiTestResult.title">OpenAPI Test Results</Translate>
          </dt>
          <dd>
            {qualityGateEntity.apiTests && qualityGateEntity.apiTests.length > 0 ? (
              <ApiTestTable apiTests={qualityGateEntity.apiTests} />
            ) : (
              !loading && (
                <div className="alert alert-warning">
                  <Translate contentKey="snowWhiteApp.qualityGate.home.notFound">No Quality Gates found</Translate>
                </div>
              )
            )}
          </dd>
        </dl>
        <Button tag={Link} to="/quality-gate" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <a href={`/api/rest/v1/reports/${qualityGateEntity.calculationId}/junit`}>
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
