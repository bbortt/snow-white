/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router';
import { screen } from '@testing-library/dom';

import initStore from 'app/config/store';
import Header from './header';

describe('Header', () => {
  let mountedWrapper;
  const devProps = {
    isAuthenticated: true,
    isAdmin: true,
    currentLocale: 'en',
    ribbonEnv: 'dev',
    isInProduction: false,
    isOpenAPIEnabled: true,
  };
  const prodProps = {
    ...devProps,
    ribbonEnv: 'prod',
    isInProduction: true,
    isOpenAPIEnabled: false,
  };
  const userProps = {
    ...prodProps,
    isAdmin: false,
  };
  const guestProps = {
    ...prodProps,
    isAdmin: false,
    isAuthenticated: false,
  };

  const wrapper = (props = devProps) => {
    if (!mountedWrapper) {
      const store = initStore();
      const { container } = render(
        <Provider store={store}>
          <MemoryRouter>
            <Header {...props} />
          </MemoryRouter>
        </Provider>,
      );
      mountedWrapper = container.innerHTML;
    }
    return mountedWrapper;
  };

  beforeEach(() => {
    mountedWrapper = undefined;
  });

  it('Renders a Header component in dev profile with LoadingBar, Navbar, Nav and dev ribbon.', () => {
    wrapper();

    // Ribbon
    expect(screen.getByTestId('dev-ribbon')).toBeVisible();
    // Find Navbar component
    expect(screen.getByTestId('navbar')).toBeVisible();
    // Language Menu
    expect(screen.getByTestId('locale-menu')).toBeVisible();

    // Basic Nav Items
    expect(screen.getByTestId('quality-gates-menu')).toBeVisible();
    expect(screen.getByTestId('criteria-menu')).toBeVisible();

    // Resources Menu
    expect(screen.getByTestId('resources-menu')).toBeVisible();
  });

  it('Renders a Header component in prod profile with LoadingBar, Navbar, Nav.', () => {
    wrapper(prodProps);

    // Ribbon should *not* be displayed
    expect(screen.queryByTestId('dev-ribbon')).toBeNull();

    // Find Navbar component
    expect(screen.getByTestId('navbar')).toBeVisible();
    // Language Menu
    expect(screen.getByTestId('locale-menu')).toBeVisible();

    // Basic Nav Items
    expect(screen.getByTestId('quality-gates-menu')).toBeVisible();
    expect(screen.getByTestId('criteria-menu')).toBeVisible();

    // Resources Menu
    expect(screen.getByTestId('resources-menu')).toBeVisible();
  });
});
