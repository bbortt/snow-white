/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { SORT } from 'app/shared/util/pagination.constants';
import React, { useEffect } from 'react';
import { Translate } from 'react-jhipster';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';

import { getEntities } from './open-api-criterion.reducer';

export const OpenApiCriterion = () => {
  const dispatch = useAppDispatch();

  const openApiCriterionList = useAppSelector(state => state.snowwhite.openApiCriterion.entities);
  const loading = useAppSelector(state => state.snowwhite.openApiCriterion.loading);

  const getAllEntities = () => {
    dispatch(getEntities());
  };

  const handleSyncList = () => {
    getAllEntities();
  };

  useEffect(() => {
    handleSyncList();
  }, []);

  return (
    <div>
      <h2 id="open-api-criterion-heading" data-testid="OpenApiCriterionHeading">
        <Translate contentKey="snowWhiteApp.openApiCriterion.home.title">Open Api Criteria</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="snowWhiteApp.openApiCriterion.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        {openApiCriterionList && openApiCriterionList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th>
                  <Translate contentKey="snowWhiteApp.openApiCriterion.name">Name</Translate>{' '}
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.openApiCriterion.description">Description</Translate>{' '}
                </th>
              </tr>
            </thead>
            <tbody>
              {openApiCriterionList.map((openApiCriterion, i) => (
                <tr key={`entity-${i}`} data-testid="entityTable">
                  <td>{openApiCriterion.name}</td>
                  <td>{openApiCriterion.description}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="snowWhiteApp.openApiCriterion.home.notFound">No Open Api Criteria found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default OpenApiCriterion;
