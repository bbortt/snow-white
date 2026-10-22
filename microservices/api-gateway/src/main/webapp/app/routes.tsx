/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import EntitiesRoutes from 'app/entities/routes';
import Home from 'app/modules/home/home';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import React from 'react';
import { Route } from 'react-router';

const AppRoutes = () => {
  return (
    <div className="view-routes">
      <ErrorBoundaryRoutes>
        <Route index element={<Home />} />
        {/* Unmatched paths fall through to EntitiesRoutes, which owns the not-found fallback. */}
        <Route path="*" element={<EntitiesRoutes />} />
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default AppRoutes;
