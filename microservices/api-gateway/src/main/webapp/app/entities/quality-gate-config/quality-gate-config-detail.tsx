import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './quality-gate-config.reducer';

export const QualityGateConfigDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const qualityGateConfigEntity = useAppSelector(state => state.snowwhite.qualityGateConfig.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="qualityGateConfigDetailsHeading">
          <Translate contentKey="snowWhiteApp.qualityGateConfig.detail.title">QualityGateConfig</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{qualityGateConfigEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="snowWhiteApp.qualityGateConfig.name">Name</Translate>
            </span>
          </dt>
          <dd>{qualityGateConfigEntity.name}</dd>
          <dt>
            <span id="description">
              <Translate contentKey="snowWhiteApp.qualityGateConfig.description">Description</Translate>
            </span>
          </dt>
          <dd>{qualityGateConfigEntity.description}</dd>
          <dt>
            <span id="isPredefined">
              <Translate contentKey="snowWhiteApp.qualityGateConfig.isPredefined">Is Predefined</Translate>
            </span>
          </dt>
          <dd>{qualityGateConfigEntity.isPredefined ? 'true' : 'false'}</dd>
          <dt>
            <Translate contentKey="snowWhiteApp.qualityGateConfig.openApiCriteria">Open Api Criteria</Translate>
          </dt>
          <dd>
            {qualityGateConfigEntity.openApiCriteria
              ? qualityGateConfigEntity.openApiCriteria.map((val, i) => (
                  <span key={val.id}>
                    <a>{val.name}</a>
                    {qualityGateConfigEntity.openApiCriteria && i === qualityGateConfigEntity.openApiCriteria.length - 1 ? '' : ', '}
                  </span>
                ))
              : null}
          </dd>
        </dl>
        <Button tag={Link} to="/quality-gate-config" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/quality-gate-config/${qualityGateConfigEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default QualityGateConfigDetail;
