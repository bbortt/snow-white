/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getOpenApiCriteria } from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import { mapIdList } from 'app/shared/util/entity-utils';
import React, { useEffect } from 'react';
import { Translate, translate, ValidatedField, ValidatedForm } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';

import { createEntity, getEntity, reset, updateEntity } from './quality-gate-config.reducer';

export const QualityGateUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const openApiCriteriaList: IOpenApiCriterion[] = useAppSelector(state => state.snowwhite.openApiCriterion.entities);
  const qualityGateConfigEntity: IQualityGateConfig = useAppSelector(state => state.snowwhite.qualityGateConfig.entity);
  const loading = useAppSelector(state => state.snowwhite.qualityGateConfig.loading);
  const updating = useAppSelector(state => state.snowwhite.qualityGateConfig.updating);
  const updateSuccess = useAppSelector(state => state.snowwhite.qualityGateConfig.updateSuccess);

  const handleClose = () => {
    navigate(`/quality-gate-config${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getOpenApiCriteria());
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    const entity = {
      ...qualityGateConfigEntity,
      ...values,
      isPredefined: false,
      openApiCriteria: mapIdList(values.openApiCriteria),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          ...qualityGateConfigEntity,
          isPredefined: false,
          openApiCriteria: qualityGateConfigEntity?.openApiCriteria?.map(openApiCriterion => openApiCriterion.name),
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="snowWhiteApp.qualityGateConfig.heading" data-testid="Quality-GateCreateUpdateHeading">
            {isNew ? (
              <Translate contentKey="snowWhiteApp.qualityGateConfig.home.createLabel">Create a new Quality-Gate</Translate>
            ) : (
              <Translate contentKey="snowWhiteApp.qualityGateConfig.home.editLabel">Edit Quality-Gate</Translate>
            )}
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="quality-gate-config-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.name')}
                id="quality-gate-config-name"
                name="name"
                data-testid="name"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.description')}
                id="quality-gate-config-description"
                name="description"
                data-testid="description"
                type="text"
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.isPredefined')}
                id="quality-gate-config-isPredefined"
                name="isPredefined"
                data-testid="isPredefined"
                check
                type="checkbox"
                readOnly={true}
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.openApiCriteria')}
                id="quality-gate-config-openApiCriteria"
                data-testid="openApiCriteria"
                type="select"
                multiple
                name="openApiCriteria"
              >
                <option value="" key="0" />
                {openApiCriteriaList
                  ? openApiCriteriaList.map(otherEntity => (
                      <option value={otherEntity.name} key={otherEntity.name}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-testid="entityCreateCancelButton" onClick={() => navigate(-1)} replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-testid="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default QualityGateUpdate;
