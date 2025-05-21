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

/* eslint-disable */
// entitiesReducers must be imported upfront!
import entitiesReducers from './reducers';
import OpenApiCriterion from './open-api-criterion';
import QualityGate from 'app/entities/quality-gate';
import QualityGateConfig from './quality-gate-config';
/* eslint-enable */

export const EntitiesRoutes = () => {
  const store = getStore();
  store.injectReducer('snowwhite', combineReducers(entitiesReducers as ReducersMapObject));

  return (
    <div>
      <ErrorBoundaryRoutes>
        {/* prettier-ignore */}
        <Route path="open-api-criterion/*" element={<OpenApiCriterion />} />
        <Route path="quality-gate/*" element={<QualityGate />} />
        <Route path="quality-gate-config/*" element={<QualityGateConfig />} />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default EntitiesRoutes;
