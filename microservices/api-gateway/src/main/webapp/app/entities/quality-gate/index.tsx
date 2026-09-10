/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router';

import QualityGateDetail from './quality-gate-detail';
import QualityGateResults from './quality-gate-results';

const QualityGateRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<QualityGateResults />} />
    <Route path=":id">
      <Route index element={<QualityGateDetail />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default QualityGateRoutes;
