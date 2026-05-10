/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { useAnimatedList } from 'app/shared/use-animated-list';
import React, { createRef, useEffect, useRef } from 'react';
import { Translate } from 'react-jhipster';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import { Badge, Button, Table } from 'reactstrap';
import 'app/shared/table-row-animation.scss';

import { getEntities } from './api-index.reducer';

export const ApiIndex = () => {
  const dispatch = useAppDispatch();

  const apiList = useAppSelector(state => state.snowwhite.apiIndex.entities);
  const loading = useAppSelector(state => state.snowwhite.apiIndex.loading);

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());

  const { displayedList, isExiting } = useAnimatedList([...apiList], api => `${api.serviceName}-${api.apiName}-${api.apiVersion}`);

  const handleSyncList = () => {
    dispatch(getEntities());
  };

  useEffect(() => {
    handleSyncList();
  }, []);

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
        {displayedList && displayedList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th>
                  <Translate contentKey="snowWhiteApp.apiIndex.serviceName">Service Name</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.apiIndex.apiName">API Name</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.apiIndex.apiVersion">Version</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.apiIndex.apiType">Type</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.apiIndex.prerelease">Prerelease</Translate>
                </th>
              </tr>
            </thead>
            <TransitionGroup component="tbody" appear>
              {displayedList.map((api, i) => {
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
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="snowWhiteApp.apiIndex.home.notFound">No APIs found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default ApiIndex;
