/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import React, { useEffect } from 'react';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';

import { getEntity } from './quality-gate.reducer';

export const QualityGateDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const qualityGateEntity = useAppSelector(state => state.snowwhite.qualityGate.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="qualityGateDetailsHeading">
          <Translate contentKey="snowWhiteApp.qualityGate.detail.title">QualityGate</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{qualityGateEntity.id}</dd>
          <dt>
            <span id="calculationId">
              <Translate contentKey="snowWhiteApp.qualityGate.calculationId">Calculation Id</Translate>
            </span>
          </dt>
          <dd>{qualityGateEntity.calculationId}</dd>
          <dt>
            <span id="status">
              <Translate contentKey="snowWhiteApp.qualityGate.status">Status</Translate>
            </span>
          </dt>
          <dd>{qualityGateEntity.status}</dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="snowWhiteApp.qualityGate.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>
            {qualityGateEntity.createdAt ? <TextFormat value={qualityGateEntity.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <Translate contentKey="snowWhiteApp.qualityGate.calculationRequest">Calculation Request</Translate>
          </dt>
          <dd>{qualityGateEntity.calculationRequest ? qualityGateEntity.calculationRequest.apiName : ''}</dd>
        </dl>
        <Button tag={Link} to="/quality-gate" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/quality-gate/${qualityGateEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default QualityGateDetail;
