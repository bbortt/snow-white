/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ReducersMapObject } from '@reduxjs/toolkit';

import { combineReducers } from '@reduxjs/toolkit';
import getStore from 'app/config/store';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router-dom';

import OpenApiCriterion from './open-api-criterion';
import QualityGateConfig from './quality-gate-config';
import entitiesReducers from './reducers';
/* jhipster-needle-add-route-import - JHipster will add routes here */

export const EntitiesRoutes = () => {
  const store = getStore();
  store.injectReducer('snowwhite', combineReducers(entitiesReducers as ReducersMapObject));

  return (
    <div>
      <ErrorBoundaryRoutes>
        {/* prettier-ignore */}
        <Route path="open-api-criterion/*" element={<OpenApiCriterion />} />
        <Route path="quality-gate-config/*" element={<QualityGateConfig />} />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default EntitiesRoutes;
