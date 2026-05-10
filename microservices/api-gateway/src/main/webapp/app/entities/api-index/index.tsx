/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router-dom';

import ApiIndex from './api-index';

const ApiIndexRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<ApiIndex />} />
  </ErrorBoundaryRoutes>
);

export default ApiIndexRoutes;
