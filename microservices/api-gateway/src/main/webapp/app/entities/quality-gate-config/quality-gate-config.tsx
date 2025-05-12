/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

import { faSort, faSortUp, faSortDown } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import React, { useState, useEffect } from 'react';
import { Translate, getPaginationState, JhiPagination, JhiItemCount } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';

import { getEntities } from './quality-gate-config.reducer';

export const QualityGateConfig = () => {
  const dispatch = useAppDispatch();

  const location = useLocation();
  const navigate = useNavigate();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getPaginationState(location, ITEMS_PER_PAGE, 'name'), location.search),
  );

  const qualityGateConfigList: IQualityGateConfig[] = useAppSelector(state => state.snowwhite.qualityGateConfig.entities);
  const loading = useAppSelector(state => state.snowwhite.qualityGateConfig.loading);
  const totalItems = useAppSelector(state => state.snowwhite.qualityGateConfig.totalItems);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${paginationState.sort},${paginationState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`;
    if (location.search !== endURL) {
      navigate(`${location.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const page = params.get('page');
    const sort = params.get(SORT);
    if (page && sort) {
      const sortSplit = sort.split(',');
      setPaginationState({
        ...paginationState,
        activePage: +page,
        sort: sortSplit[0],
        order: sortSplit[1],
      });
    }
  }, [location.search]);

  const sort = p => () => {
    setPaginationState({
      ...paginationState,
      order: paginationState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handlePagination = currentPage => {
    setPaginationState({
      ...paginationState,
      activePage: currentPage,
    });
  };

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = paginationState.sort;
    const order = paginationState.order;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="quality-gate-config-heading" data-testid="QualityGateConfigHeading">
        <Translate contentKey="snowWhiteApp.qualityGateConfig.home.title">Quality Gate Configs</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="snowWhiteApp.qualityGateConfig.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link
            to="/quality-gate-config/new"
            className="btn btn-primary jh-create-entity"
            id="jh-create-entity"
            data-testid="entityCreateButton"
          >
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="snowWhiteApp.qualityGateConfig.home.createLabel">Create new Quality Gate Config</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {qualityGateConfigList && qualityGateConfigList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('name')}>
                  <Translate contentKey="snowWhiteApp.qualityGateConfig.name">Name</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('name')} />
                </th>
                <th className="hand" onClick={sort('description')}>
                  <Translate contentKey="snowWhiteApp.qualityGateConfig.description">Description</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('description')} />
                </th>
                <th className="hand" onClick={sort('isPredefined')}>
                  <Translate contentKey="snowWhiteApp.qualityGateConfig.isPredefined">Is Predefined</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('isPredefined')} />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {qualityGateConfigList.map((qualityGateConfig: IQualityGateConfig, i) => (
                <tr key={`entity-${i}`} data-testid="entityTable">
                  <td>
                    <Button tag={Link} to={`/quality-gate-config/${qualityGateConfig.name}`} color="link" size="sm">
                      {qualityGateConfig.name}
                    </Button>
                  </td>
                  <td>{qualityGateConfig.description}</td>
                  <td>{qualityGateConfig.isPredefined ? 'true' : 'false'}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button
                        tag={Link}
                        to={`/quality-gate-config/${qualityGateConfig.name}`}
                        color="info"
                        size="sm"
                        data-testid="entityDetailsButton"
                      >
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/quality-gate-config/${qualityGateConfig.name}/edit?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`}
                        color="primary"
                        size="sm"
                        data-testid="entityEditButton"
                      >
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      {qualityGateConfig.isPredefined ? (
                        <></>
                      ) : (
                        <Button
                          tag={Link}
                          to={`/quality-gate-config/${qualityGateConfig.name}/delete?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`}
                          color="danger"
                          size="sm"
                          data-testid="entityDeleteButton"
                        >
                          <FontAwesomeIcon icon="trash" />{' '}
                          <span className="d-none d-md-inline">
                            <Translate contentKey="entity.action.delete">Delete</Translate>
                          </span>
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="snowWhiteApp.qualityGateConfig.home.notFound">No Quality Gate Configs found</Translate>
            </div>
          )
        )}
      </div>
      {totalItems ? (
        <div className={qualityGateConfigList && qualityGateConfigList.length > 0 ? '' : 'd-none'}>
          <div className="justify-content-center d-flex">
            <JhiItemCount page={paginationState.activePage} total={totalItems} itemsPerPage={paginationState.itemsPerPage} i18nEnabled />
          </div>
          <div className="justify-content-center d-flex">
            <JhiPagination
              activePage={paginationState.activePage}
              onSelect={handlePagination}
              maxButtons={5}
              itemsPerPage={paginationState.itemsPerPage}
              totalItems={totalItems}
            />
          </div>
        </div>
      ) : (
        ''
      )}
    </div>
  );
};

export default QualityGateConfig;
