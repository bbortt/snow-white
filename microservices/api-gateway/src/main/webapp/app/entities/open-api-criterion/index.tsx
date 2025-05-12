/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router-dom';

import OpenApiCriterion from './open-api-criterion';
import OpenApiCriterionDetail from './open-api-criterion-detail';

const OpenApiCriterionRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<OpenApiCriterion />} />
    <Route path=":id">
      <Route index element={<OpenApiCriterionDetail />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default OpenApiCriterionRoutes;
