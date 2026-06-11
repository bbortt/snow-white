/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { GetAllApis200ResponseInner } from 'app/clients/api-index-api';
import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { useAnimatedList } from 'app/shared/use-animated-list';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import React, { createRef, ReactElement, useEffect, useMemo, useRef, useState } from 'react';
import { JhiItemCount, JhiPagination, Translate, getPaginationState, translate } from 'react-jhipster';
import { useLocation, useNavigate } from 'react-router-dom';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import 'app/shared/table-row-animation.scss';
import { Badge, Button, Input, Table } from 'reactstrap';

import { getEntities } from './api-index.reducer';

export const ApiIndex = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const apiList: GetAllApis200ResponseInner[] = useAppSelector(state => state.snowwhite.apiIndex.entities);
  const loading = useAppSelector(state => state.snowwhite.apiIndex.loading);
  const totalItems = useAppSelector(state => state.snowwhite.apiIndex.totalItems);

  // Immediate input state (controlled inputs)
  const [inputServiceName, setInputServiceName] = useState('');
  const [inputApiName, setInputApiName] = useState('');
  const [inputApiVersion, setInputApiVersion] = useState('');

  // Debounced state driving server-side filter params
  const [filterServiceName, setFilterServiceName] = useState('');
  const [filterApiName, setFilterApiName] = useState('');

  const paginationAndSortingEnabled = useMemo(() => {
    return filterServiceName !== '' || filterApiName !== '' || !!totalItems;
  }, [totalItems, filterServiceName, filterApiName]);

  const paginationBaseState = getPaginationState(pageLocation, ITEMS_PER_PAGE, 'otelServiceName', 'asc');
  const [paginationState, setPaginationState] = useState(
    paginationAndSortingEnabled ? overridePaginationStateWithQueryParams(paginationBaseState, pageLocation.search) : paginationBaseState,
  );

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());

  const { displayedList, isExiting } = useAnimatedList(apiList, api => `${api.serviceName}-${api.apiName}-${api.apiVersion}`);

  const filteredList = useMemo(() => {
    if (!inputApiVersion) return displayedList;
    const lower = inputApiVersion.toLowerCase();
    return displayedList.filter(api => api.apiVersion.toLowerCase().includes(lower));
  }, [displayedList, inputApiVersion]);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${paginationState.sort},${paginationState.order}`,
        serviceName: filterServiceName || undefined,
        apiName: filterApiName || undefined,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`;
    if (paginationAndSortingEnabled && pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort, filterServiceName, filterApiName]);

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

  // Debounce service name — changing it also resets the API name filter
  useEffect(() => {
    const t = setTimeout(() => {
      setFilterServiceName(inputServiceName);
      setInputApiName('');
      setFilterApiName('');
      setPaginationState(prev => ({ ...prev, activePage: 1 }));
    }, 300);
    return () => clearTimeout(t);
  }, [inputServiceName]);

  // Debounce API name filter
  useEffect(() => {
    const t = setTimeout(() => {
      setFilterApiName(inputApiName);
      setPaginationState(prev => ({ ...prev, activePage: 1 }));
    }, 300);
    return () => clearTimeout(t);
  }, [inputApiName]);

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

  const getSortIconByFieldName = (fieldName: string): IconDefinition => {
    const sortFieldName = paginationState.sort;
    const order = paginationState.order;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  const getTableHeaderRow = (contentKey: string, defaultHeader: string, fieldName: string): ReactElement => {
    return paginationAndSortingEnabled ? (
      <th className="hand" onClick={sort(fieldName)}>
        <Translate contentKey={contentKey}>{defaultHeader}</Translate>
        <FontAwesomeIcon icon={getSortIconByFieldName(fieldName)} />
      </th>
    ) : (
      <th>
        <Translate contentKey={contentKey}>{defaultHeader}</Translate>
      </th>
    );
  };

  const getFilterableHeaderRow = (
    contentKey: string,
    defaultHeader: string,
    fieldName: string,
    filterValue: string,
    onFilterChange: (value: string) => void,
  ): ReactElement => (
    <th>
      <div className="hand d-flex align-items-center column-gap-1" onClick={sort(fieldName)}>
        {filterValue && <Translate contentKey={contentKey}>{defaultHeader}</Translate>}
        <Input
          type="text"
          bsSize="sm"
          className="mt-1"
          value={filterValue}
          onChange={e => onFilterChange(e.target.value)}
          placeholder={translate(contentKey, {}, defaultHeader)}
        />
        <FontAwesomeIcon icon={getSortIconByFieldName(fieldName)} />
      </div>
    </th>
  );

  return (
    <div>
      <h2 id="api-index-heading" data-testid="ApiIndexHeading">
        <Translate contentKey="snowWhiteApp.apiIndex.home.title">API Index</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="snowWhiteApp.apiIndex.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        <Table responsive>
          <thead>
            <tr>
              {getFilterableHeaderRow(
                'snowWhiteApp.apiIndex.serviceName',
                'Service Name',
                'otelServiceName',
                inputServiceName,
                setInputServiceName,
              )}
              {getFilterableHeaderRow('snowWhiteApp.apiIndex.apiName', 'API Name', 'apiName', inputApiName, setInputApiName)}
              {getFilterableHeaderRow('snowWhiteApp.apiIndex.apiVersion', 'Version', 'apiVersion', inputApiVersion, setInputApiVersion)}
              {getTableHeaderRow('snowWhiteApp.apiIndex.apiType', 'Type', 'apiType')}
              <th>
                <Translate contentKey="snowWhiteApp.apiIndex.prerelease">Prerelease</Translate>
              </th>
            </tr>
          </thead>
          <TransitionGroup component="tbody" appear>
            {filteredList.map((api, i) => {
              const key = `entity-${api.serviceName}-${api.apiName}-${api.apiVersion}`;
              if (!nodeRefs.current.has(key)) {
                nodeRefs.current.set(key, createRef<HTMLTableRowElement>());
              }
              const nodeRef = nodeRefs.current.get(key)!;

              return (
                <CSSTransition
                  key={key}
                  timeout={{ enter: CSS_TRANSITION_TIMEOUT + i * 30, exit: 0, appear: CSS_TRANSITION_TIMEOUT + i * 30 }}
                  classNames="table-row"
                  nodeRef={nodeRef}
                  appear
                >
                  <tr
                    ref={nodeRef}
                    data-testid="apiIndexTable"
                    className={isExiting ? 'table-row-exit-active' : undefined}
                    style={{ transitionDelay: `${i * 30}ms` }}
                  >
                    <td>{api.serviceName}</td>
                    <td>{api.apiName}</td>
                    <td>{api.apiVersion}</td>
                    <td>{api.apiType}</td>
                    <td>
                      {api.prerelease && (
                        <Badge color="warning">
                          <Translate contentKey="snowWhiteApp.apiIndex.prereleaseLabel">Pre</Translate>
                        </Badge>
                      )}
                    </td>
                  </tr>
                </CSSTransition>
              );
            })}
          </TransitionGroup>
        </Table>
        {apiList && apiList.length === 0 && !loading && (
          <div className="alert alert-warning">
            <Translate contentKey="snowWhiteApp.apiIndex.home.notFound">No APIs found</Translate>
          </div>
        )}
      </div>
      {paginationAndSortingEnabled && totalItems > 0 ? (
        <div>
          <div className="justify-content-center d-flex mb-1">
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
        <></>
      )}
    </div>
  );
};

export default ApiIndex;
