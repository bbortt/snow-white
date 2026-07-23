/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ReducersMapObject } from '@reduxjs/toolkit';

import { combineReducers } from '@reduxjs/toolkit';
import getStore from 'app/config/store';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router';

/* eslint-disable perfectionist/sort-imports -- entitiesReducers must be imported upfront! */
import entitiesReducers from './reducers';
import ApiIndex from 'app/entities/api-index';
import OpenApiCriterion from 'app/entities/open-api-criterion';
import QualityGate from 'app/entities/quality-gate';
import QualityGateConfig from 'app/entities/quality-gate-config';
/* eslint-enable perfectionist/sort-imports */

// Inject reducers at module load time so the store is ready before any component renders.
getStore().injectReducer('snowwhite', combineReducers(entitiesReducers as ReducersMapObject));

export const EntitiesRoutes = () => {
  return (
    <div>
      <ErrorBoundaryRoutes>
        {/* prettier-ignore */}
        <Route path="api-index/*" element={<ApiIndex />} />
        <Route path="open-api-criterion/*" element={<OpenApiCriterion />} />
        <Route path="quality-gate/*" element={<QualityGate />} />
        <Route path="quality-gate-config/*" element={<QualityGateConfig />} />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default EntitiesRoutes;
