/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import React, { useEffect } from 'react';
import { Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';

import { getEntity } from './open-api-criterion.reducer';

export const OpenApiCriterionDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const openApiCriterionEntity = useAppSelector(state => state.snowwhite.openApiCriterion.entity);

  return (
    <Row>
      <Col>
        <h2 data-testid="openApiCriterionDetailsHeading">
          <Translate contentKey="snowWhiteApp.openApiCriterion.detail.title">OpenApiCriterion</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{openApiCriterionEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="snowWhiteApp.openApiCriterion.name">Name</Translate>
            </span>
          </dt>
          <dd>{openApiCriterionEntity.name}</dd>
          <dt>
            <span id="description">
              <Translate contentKey="snowWhiteApp.openApiCriterion.description">Description</Translate>
            </span>
          </dt>
          <dd>{openApiCriterionEntity.description}</dd>
        </dl>
        <Button tag={Link} to="/open-api-criterion" replace color="info" data-testid="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/open-api-criterion/${openApiCriterionEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default OpenApiCriterionDetail;
