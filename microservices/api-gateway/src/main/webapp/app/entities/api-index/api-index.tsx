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

  // Immediate input state (controlled inputs) — initialized from URL on mount for shareable links
  const [inputServiceName, setInputServiceName] = useState(() => new URLSearchParams(pageLocation.search).get('serviceName') ?? '');
  const [inputApiName, setInputApiName] = useState(() => new URLSearchParams(pageLocation.search).get('apiName') ?? '');
  const [inputApiVersion, setInputApiVersion] = useState(() => new URLSearchParams(pageLocation.search).get('apiVersion') ?? '');

  // Debounced state driving server-side filter params — mirrors input state on mount
  const [filterServiceName, setFilterServiceName] = useState(() => new URLSearchParams(pageLocation.search).get('serviceName') ?? '');
  const [filterApiName, setFilterApiName] = useState(() => new URLSearchParams(pageLocation.search).get('apiName') ?? '');

  const paginationAndSortingEnabled = useMemo(() => {
    return filterServiceName !== '' || filterApiName !== '' || !!totalItems;
  }, [totalItems, filterServiceName, filterApiName]);

  const paginationBaseState = getPaginationState(pageLocation, ITEMS_PER_PAGE, 'otelServiceName', 'asc');
  const [paginationState, setPaginationState] = useState(
    paginationAndSortingEnabled ? overridePaginationStateWithQueryParams(paginationBaseState, pageLocation.search) : paginationBaseState,
  );

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());
  // Tracks self-initiated navigations so the URL read-back effect doesn't clobber in-progress input
  const selfNavigatingRef = useRef(false);
  // Tracks the current URL for use in the apiVersion URL sync effect
  const currentSearchRef = useRef(pageLocation.search);
  currentSearchRef.current = pageLocation.search;
  // Tracks the previous debounce input values to skip the cascading reset on initial mount
  const prevInputServiceNameRef = useRef(inputServiceName);
  const prevInputApiNameRef = useRef(inputApiName);

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
    const urlParams = new URLSearchParams();
    urlParams.set('page', String(paginationState.activePage));
    urlParams.set('sort', `${paginationState.sort},${paginationState.order}`);
    if (filterServiceName) urlParams.set('serviceName', filterServiceName);
    if (filterApiName) urlParams.set('apiName', filterApiName);
    if (inputApiVersion) urlParams.set('apiVersion', inputApiVersion);
    const endURL = `?${urlParams.toString()}`;
    if ((paginationAndSortingEnabled || !!pageLocation.search) && pageLocation.search !== endURL) {
      selfNavigatingRef.current = true;
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort, filterServiceName, filterApiName]);

  useEffect(() => {
    // Skip on self-navigations — we just set this URL, no need to read it back
    if (selfNavigatingRef.current) {
      selfNavigatingRef.current = false;
      return;
    }
    // Sync state from URL for external navigation (back/forward, shared links)
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
    const urlServiceName = params.get('serviceName') ?? '';
    const urlApiName = params.get('apiName') ?? '';
    // Update prevRefs before state so the debounce effects see no change and skip the cascade
    prevInputServiceNameRef.current = urlServiceName;
    prevInputApiNameRef.current = urlApiName;
    setInputServiceName(urlServiceName);
    setFilterServiceName(urlServiceName);
    setInputApiName(urlApiName);
    setFilterApiName(urlApiName);
    setInputApiVersion(params.get('apiVersion') ?? '');
  }, [pageLocation.search]);

  // Debounce service name — changing it also resets the API name filter
  // prevRef guard skips the cascade on initial mount (both start at the URL-initialized value)
  useEffect(() => {
    const prev = prevInputServiceNameRef.current;
    prevInputServiceNameRef.current = inputServiceName;
    if (prev === inputServiceName) return;
    const t = setTimeout(() => {
      setFilterServiceName(inputServiceName);
      setInputApiName('');
      setFilterApiName('');
      setPaginationState(p => ({ ...p, activePage: 1 }));
    }, 300);
    return () => clearTimeout(t);
  }, [inputServiceName]);

  // Debounce API name filter
  useEffect(() => {
    const prev = prevInputApiNameRef.current;
    prevInputApiNameRef.current = inputApiName;
    if (prev === inputApiName) return;
    const t = setTimeout(() => {
      setFilterApiName(inputApiName);
      setPaginationState(p => ({ ...p, activePage: 1 }));
    }, 300);
    return () => clearTimeout(t);
  }, [inputApiName]);

  // Sync apiVersion (client-side only) to URL using replace to avoid cluttering history
  useEffect(() => {
    const params = new URLSearchParams(currentSearchRef.current);
    if (inputApiVersion) {
      params.set('apiVersion', inputApiVersion);
    } else {
      params.delete('apiVersion');
    }
    const newSearch = params.toString() ? `?${params.toString()}` : '';
    if (currentSearchRef.current !== newSearch) {
      selfNavigatingRef.current = true;
      navigate(`${pageLocation.pathname}${newSearch}`, { replace: true });
    }
  }, [inputApiVersion]);

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
