/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router-dom';

import QualityGateConfig from './quality-gate-config';
import QualityGateConfigDeleteDialog from './quality-gate-config-delete-dialog';
import QualityGateConfigDetail from './quality-gate-config-detail';
import QualityGateConfigUpdate from './quality-gate-config-update';

const QualityGateConfigRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<QualityGateConfig />} />
    <Route path="new" element={<QualityGateConfigUpdate />} />
    <Route path=":id">
      <Route index element={<QualityGateConfigDetail />} />
      <Route path="edit" element={<QualityGateConfigUpdate />} />
      <Route path="delete" element={<QualityGateConfigDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default QualityGateConfigRoutes;
