/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import AppComponent from 'app/app';
import { loadIcons } from 'app/config/icon-loader';
import getStore from 'app/config/store';
import { registerLocale } from 'app/config/translation';
import ErrorBoundary from 'app/shared/error/error-boundary';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';

const store = getStore();
registerLocale(store);

loadIcons();

const rootEl = document.getElementById('root');
const root = createRoot(rootEl!);

const render = Component => {
  root.render(
    <ErrorBoundary>
      <Provider store={store}>
        <div>
          <Component />
        </div>
      </Provider>
    </ErrorBoundary>,
  );
};

render(AppComponent);
