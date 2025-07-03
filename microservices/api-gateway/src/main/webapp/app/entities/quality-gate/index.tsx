/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router';

import QualityGate from './quality-gate';
import QualityGateDetail from './quality-gate-detail';

const QualityGateRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<QualityGate />} />
    <Route path=":id">
      <Route index element={<QualityGateDetail />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default QualityGateRoutes;
