/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import 'react-toastify/dist/ReactToastify.css';

import './app.scss';

import 'app/config/dayjs';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import AppRoutes from 'app/routes';
import ErrorBoundary from 'app/shared/error/error-boundary';
import Footer from 'app/shared/layout/footer/footer';
import Header from 'app/shared/layout/header/header';
import { getProfile } from 'app/shared/reducers/application-profile';
import React, { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import { Card } from 'reactstrap';

const baseHref = document.querySelector('base')?.getAttribute('href')?.replace(/\/$/, '');

export const App = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getProfile());
  }, []);

  const currentLocale = useAppSelector(state => state.locale.currentLocale);
  const ribbonEnv = useAppSelector(state => state.applicationProfile.ribbonEnv);
  const isInProduction = useAppSelector(state => state.applicationProfile.inProduction);

  const paddingTop = '60px';
  return (
    <BrowserRouter basename={baseHref}>
      <div className="app-container" style={{ paddingTop }}>
        <ToastContainer position="top-left" className="toastify-container" toastClassName="toastify-toast" />
        <ErrorBoundary>
          <Header currentLocale={currentLocale} ribbonEnv={ribbonEnv} isInProduction={isInProduction} />
        </ErrorBoundary>
        <div className="container-fluid view-container" id="app-view-container">
          <Card className="jh-card">
            <ErrorBoundary>
              <AppRoutes />
            </ErrorBoundary>
          </Card>
          <Footer />
        </div>
      </div>
    </BrowserRouter>
  );
};

export default App;
