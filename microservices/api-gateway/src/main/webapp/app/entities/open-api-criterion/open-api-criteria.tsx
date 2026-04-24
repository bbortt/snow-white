/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import { TextWithCode } from 'app/shared/TextWithCode';
import { useAnimatedList } from 'app/shared/use-animated-list';
import React, { createRef, useEffect, useRef } from 'react';
import { Translate, translate } from 'react-jhipster';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import { Button, Table } from 'reactstrap';
import 'app/shared/table-row-animation.scss';

import { getEntities } from './open-api-criterion.reducer';

export const OpenApiCriteria = () => {
  const dispatch = useAppDispatch();

  const openApiCriterionList: IOpenApiCriterion[] | undefined = useAppSelector(state => state.snowwhite.openApiCriterion.entities);

  const nodeRefs = useRef<Map<string, React.RefObject<HTMLTableRowElement | null>>>(new Map());

  const { displayedList, isExiting } = useAnimatedList(openApiCriterionList ?? [], q => q.name!);
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
        <Translate contentKey="snowWhiteApp.openApiCriterion.home.title">OpenApi Criteria</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="snowWhiteApp.openApiCriterion.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        {displayedList && displayedList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th>
                  <Translate contentKey="snowWhiteApp.openApiCriterion.name">Coverage Type</Translate>
                </th>
                <th>
                  <Translate contentKey="snowWhiteApp.openApiCriterion.descriptionHeader">Description</Translate>
                </th>
              </tr>
            </thead>
            <TransitionGroup component="tbody" appear>
              {displayedList.map((openApiCriterion, i) => {
                const name = translate(`snowWhiteApp.openApiCriterion.description.${openApiCriterion.name}.name`);
                const description = translate(`snowWhiteApp.openApiCriterion.description.${openApiCriterion.name}.description`);

                const key = `entity-${openApiCriterion.name}`;
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
                      data-testid="openApiCriteriaTable"
                      className={isExiting ? 'table-row-exit-active' : undefined}
                      style={{ transitionDelay: `${i * 30}ms` }}
                    >
                      <td>{name.startsWith('translation-not-found') ? openApiCriterion.name : name}</td>
                      <td>
                        {description.startsWith('translation-not-found') ? (
                          openApiCriterion.description
                        ) : (
                          <TextWithCode text={description} />
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
              <Translate contentKey="snowWhiteApp.openApiCriterion.home.notFound">No Open Api Criteria found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default OpenApiCriteria;
