import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Row, Col, FormText } from 'reactstrap';
import { isNumber, Translate, translate, ValidatedField, ValidatedForm } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import { getEntities as getOpenApiCriteria } from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import { getEntity, updateEntity, createEntity, reset } from './quality-gate-config.reducer';

export const QualityGateConfigUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const openApiCriteria = useAppSelector(state => state.snowwhite.openApiCriterion.entities);
  const qualityGateConfigEntity = useAppSelector(state => state.snowwhite.qualityGateConfig.entity);
  const loading = useAppSelector(state => state.snowwhite.qualityGateConfig.loading);
  const updating = useAppSelector(state => state.snowwhite.qualityGateConfig.updating);
  const updateSuccess = useAppSelector(state => state.snowwhite.qualityGateConfig.updateSuccess);

  const handleClose = () => {
    navigate('/quality-gate-config' + location.search);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getOpenApiCriteria({}));
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
          openApiCriteria: qualityGateConfigEntity?.openApiCriteria?.map(e => e.id.toString()),
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="snowWhiteApp.qualityGateConfig.home.createOrEditLabel" data-cy="QualityGateConfigCreateUpdateHeading">
            <Translate contentKey="snowWhiteApp.qualityGateConfig.home.createOrEditLabel">Create or edit a QualityGateConfig</Translate>
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
                data-cy="name"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.description')}
                id="quality-gate-config-description"
                name="description"
                data-cy="description"
                type="text"
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.isPredefined')}
                id="quality-gate-config-isPredefined"
                name="isPredefined"
                data-cy="isPredefined"
                check
                type="checkbox"
              />
              <ValidatedField
                label={translate('snowWhiteApp.qualityGateConfig.openApiCriteria')}
                id="quality-gate-config-openApiCriteria"
                data-cy="openApiCriteria"
                type="select"
                multiple
                name="openApiCriteria"
              >
                <option value="" key="0" />
                {openApiCriteria
                  ? openApiCriteria.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/quality-gate-config" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
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

export default QualityGateConfigUpdate;
