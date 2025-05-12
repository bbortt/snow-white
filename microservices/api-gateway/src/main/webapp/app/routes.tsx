/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ReducersMapObject } from '@reduxjs/toolkit';

import { combineReducers } from '@reduxjs/toolkit';
import getStore from 'app/config/store';
import qualityGate from 'app/entities/quality-gate/quality-gate.reducer';
import EntitiesRoutes from 'app/entities/routes';
import Home from 'app/modules/home/home';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import PageNotFound from 'app/shared/error/page-not-found';
import React from 'react';
import { Route } from 'react-router';

const AppRoutes = () => {
  const store = getStore();
  store.injectReducer('snowwhite', combineReducers({ qualityGate } as ReducersMapObject));

  return (
    <div className="view-routes">
      <ErrorBoundaryRoutes>
        <Route index element={<Home />} />
        <Route path="*" element={<EntitiesRoutes />} />
        <Route path="*" element={<PageNotFound />} />
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default AppRoutes;
