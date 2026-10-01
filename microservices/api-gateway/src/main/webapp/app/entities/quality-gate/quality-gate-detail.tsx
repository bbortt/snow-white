/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ServiceGroup } from 'app/entities/quality-gate/quality-gate.utils';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { ApiTestServiceGroup } from 'app/entities/quality-gate/api-test-service-group';
import { QualityGateSummary } from 'app/entities/quality-gate/quality-gate-summary';
import { groupByService } from 'app/entities/quality-gate/quality-gate.utils';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React, { useEffect, useMemo, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';
import { Button, Col, FormGroup, Input, Label, Row } from 'reactstrap';

import { getEntity } from './quality-gate.reducer';

export const QualityGateDetail = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const loading = useAppSelector(state => state.snowwhite.qualityGate.loading);
  const qualityGateEntity: IQualityGate = useAppSelector(state => state.snowwhite.qualityGate.entity);

  const [showOnlyIncluded, setShowOnlyIncluded] = useState(true);

  const serviceGroups: ServiceGroup[] = useMemo(() => groupByService(qualityGateEntity.apiTests ?? []), [qualityGateEntity]);

  const { calculationId } = qualityGateEntity;
  const reportUrl = `/api/rest/v1/reports/${calculationId}`;

  return (
    <Row>
      <Col>
        <h2 data-cy="qualityGateDetailsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.detail.title">Quality-Gate Result</Translate>
        </h2>
        <QualityGateSummary qualityGate={qualityGateEntity} />
        <hr className="mt-5" />
        <div className="d-flex align-items-center justify-content-between mb-2">
          <h3 className="mb-0">
            <Translate contentKey="snowWhiteApp.apiTestResult.home.title">API Test Results</Translate>
          </h3>
          <FormGroup switch className="mb-0">
            <Input
              type="switch"
              role="switch"
              id="filter-included-switch"
              checked={showOnlyIncluded}
              onChange={() => setShowOnlyIncluded(prev => !prev)}
            />
            <Label check htmlFor="filter-included-switch">
              <Translate contentKey="snowWhiteApp.apiTestResult.showOnlyIncluded">Show only included</Translate>
            </Label>
          </FormGroup>
        </div>
        <div className="mb-2">
          <Button tag={Link} onClick={() => navigate(-1)} replace color="info" data-cy="entityDetailsBackButton">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          {calculationId && (
            <>
              &nbsp;
              <a href={`${reportUrl}/junit`}>
                <Button color="primary">
                  <FontAwesomeIcon icon="file-arrow-down" />{' '}
                  <span className="d-none d-md-inline">
                    <Translate contentKey="snowWhiteApp.qualityGate.action.junitDownload">JUnit Report Download</Translate>
                  </span>
                </Button>
              </a>
              &nbsp;
              {/* The report read's response body verbatim, so this file and the CLI's --report-output are the same bytes. */}
              <a href={reportUrl} download={`snow-white-report-${calculationId}.json`}>
                <Button color="secondary">
                  <FontAwesomeIcon icon="file-code" />{' '}
                  <span className="d-none d-md-inline">
                    <Translate contentKey="snowWhiteApp.qualityGate.action.reportDownload">Report JSON Download</Translate>
                  </span>
                </Button>
              </a>
            </>
          )}
        </div>
        {serviceGroups.length > 0
          ? serviceGroups.map((serviceGroup: ServiceGroup) => (
              <ApiTestServiceGroup
                serviceName={serviceGroup.serviceName}
                apiTests={serviceGroup.apiTests}
                showOnlyIncluded={showOnlyIncluded}
                minCoveragePercentage={qualityGateEntity.qualityGateConfig?.minCoveragePercentage}
                qualityGateTimedOut={qualityGateEntity.status === ReportStatus.TIMED_OUT}
                key={`api-test-service-${serviceGroup.serviceName}`}
              />
            ))
          : !loading && (
              <div className="alert alert-warning">
                <Translate contentKey="snowWhiteApp.qualityGate.home.notFound">No Quality Gates found</Translate>
              </div>
            )}
      </Col>
    </Row>
  );
};

export default QualityGateDetail;
