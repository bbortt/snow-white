/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTest } from 'app/shared/model/api-test.model';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { ApiTestCard } from 'app/entities/quality-gate/api-test-card';
import { QualityGateSummary } from 'app/entities/quality-gate/quality-gate-summary';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React, { useEffect, useMemo } from 'react';
import { Translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';

import { getEntity } from './quality-gate.reducer';

const compareNullable = (a?: string, b?: string): number => {
  return (a ?? '').localeCompare(b ?? '');
};

export const QualityGateDetail = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const loading = useAppSelector(state => state.snowwhite.qualityGate.loading);
  const qualityGateEntity: IQualityGate = useAppSelector(state => state.snowwhite.qualityGate.entity);

  const sortedApiTests: IApiTest[] = useMemo(
    () =>
      [...(qualityGateEntity.apiTests ?? [])].sort(
        (a, b) =>
          compareNullable(a.serviceName, b.serviceName) ||
          compareNullable(a.apiName, b.apiName) ||
          compareNullable(a.apiVersion, b.apiVersion),
      ),
    [qualityGateEntity],
  );

  return (
    <Row>
      <Col>
        <h2 data-cy="qualityGateDetailsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.detail.title">Quality-Gate Result</Translate>
        </h2>
        <QualityGateSummary qualityGate={qualityGateEntity} />
        <hr className="mt-5" />
        <h3 className="mb-2">
          <Translate contentKey="snowWhiteApp.apiTestResult.home.title">API Test Results</Translate>
        </h3>
        {qualityGateEntity.apiTests && qualityGateEntity.apiTests.length > 0
          ? sortedApiTests.map((apiTest: IApiTest) => (
              <ApiTestCard
                apiTest={apiTest}
                qualityGateStatus={qualityGateEntity.status || ReportStatus.NOT_STARTED}
                key={`api-test-${apiTest.serviceName}-${apiTest.apiName}-${apiTest.apiVersion}`}
              />
            ))
          : !loading && (
              <div className="alert alert-warning">
                <Translate contentKey="snowWhiteApp.qualityGate.home.notFound">No Quality Gates found</Translate>
              </div>
            )}
        <Button tag={Link} onClick={() => navigate(-1)} replace color="info" data-cy="entityDetailsBackButton">
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
