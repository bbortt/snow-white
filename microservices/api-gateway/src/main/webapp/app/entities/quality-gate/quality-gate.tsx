/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { IQualityGate } from 'app/shared/model/quality-gate.model';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import React, { useEffect, useState } from 'react';
import { JhiItemCount, JhiPagination, TextFormat, Translate, getPaginationState } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';

import { getEntities } from './quality-gate.reducer';

export const QualityGate = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getPaginationState(pageLocation, ITEMS_PER_PAGE, 'createdAt'), pageLocation.search),
  );

  const qualityGateList: IQualityGate[] = useAppSelector(state => state.snowwhite.qualityGate.entities);
  const loading = useAppSelector(state => state.snowwhite.qualityGate.loading);
  const totalItems = useAppSelector(state => state.snowwhite.qualityGate.totalItems);

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
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort]);

  useEffect(() => {
    const params = new URLSearchParams(pageLocation.search);
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
  }, [pageLocation.search]);

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
      <h2 id="quality-gate-heading" data-cy="QualityGateHeading">
        <Translate contentKey="snowWhiteApp.qualityGate.home.title">Quality Gates</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="snowWhiteApp.qualityGate.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        {qualityGateList && qualityGateList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('calculationId')}>
                  <Translate contentKey="snowWhiteApp.qualityGate.calculationId">Calculation Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('calculationId')} />
                </th>
                <th className="hand" onClick={sort('status')}>
                  <Translate contentKey="snowWhiteApp.qualityGate.status">Status</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('status')} />
                </th>
                <th className="hand" onClick={sort('createdAt')}>
                  <Translate contentKey="snowWhiteApp.qualityGate.createdAt">Initiated At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('createdAt')} />
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.serviceName">Service Name</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.apiName">API Name</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.qualityGate.apiVersion">API Version</Translate>
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {qualityGateList.map((qualityGate, i) => (
                <tr key={`entity-${qualityGate.calculationId}`} data-cy="qualityGateTable">
                  <td>
                    <Button tag={Link} to={`/quality-gate/${qualityGate.calculationId}`} color="link" size="sm">
                      {qualityGate.calculationId}
                    </Button>
                  </td>
                  <td>
                    <Translate contentKey={`snowWhiteApp.ReportStatus.${qualityGate.status}`} />
                  </td>
                  <td>
                    {qualityGate.createdAt ? (
                      <TextFormat type="date" value={qualityGate.createdAt.toISOString()} format={APP_DATE_FORMAT} />
                    ) : null}
                  </td>
                  <td>{qualityGate.calculationRequest?.serviceName}</td>
                  <td>{qualityGate.calculationRequest?.apiName}</td>
                  <td>{qualityGate.calculationRequest?.apiVersion}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button
                        tag={Link}
                        to={`/quality-gate/${qualityGate.calculationId}`}
                        color="info"
                        size="sm"
                        data-cy="entityDetailsButton"
                      >
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="snowWhiteApp.qualityGate.home.notFound">No Quality Gates found</Translate>
            </div>
          )
        )}
      </div>
      {totalItems ? (
        <div className={qualityGateList && qualityGateList.length > 0 ? '' : 'd-none'}>
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

export default QualityGate;
