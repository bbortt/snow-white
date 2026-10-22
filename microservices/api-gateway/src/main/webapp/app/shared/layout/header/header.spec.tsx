/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { screen } from '@testing-library/dom';
import { fireEvent, render, waitFor } from '@testing-library/react';
import initStore from 'app/config/store';
import axios from 'axios';
import React from 'react';
import { Storage } from 'react-jhipster';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router';
import sinon from 'sinon';

import Header from './header';

describe('Header', () => {
  let mountedWrapper;
  const devProps = {
    isAuthenticated: true,
    isAdmin: true,
    currentLocale: 'en',
    ribbonEnv: 'dev',
    isInProduction: false,
  };
  const prodProps = {
    ...devProps,
    ribbonEnv: 'prod',
    isInProduction: true,
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

  it('should toggle the collapsible menu when the navbar toggler is clicked', () => {
    wrapper();

    const navbar = screen.getByTestId('navbar');
    const collapse = navbar.querySelector('.collapse');
    expect(collapse).not.toHaveClass('collapsing');

    fireEvent.click(screen.getByLabelText('Menu'));

    expect(collapse).toHaveClass('collapsing');
  });

  it('should store the selected locale and dispatch setLocale when a locale is chosen', async () => {
    const originalAxiosGet = axios.get;
    axios.get = sinon.stub().returns(Promise.resolve({ data: {} }));
    const sessionSetSpy = sinon.spy(Storage.session, 'set');

    const store = initStore();
    render(
      <Provider store={store}>
        <MemoryRouter>
          <Header {...devProps} />
        </MemoryRouter>
      </Provider>,
    );

    fireEvent.click(screen.getByTestId('locale-menu').querySelector('.dropdown-toggle')!);
    fireEvent.click(screen.getByText('Deutsch'));

    expect(sessionSetSpy.calledWith('locale', 'de')).toBe(true);
    await waitFor(() => expect(store.getState().locale.currentLocale).toEqual('de'));

    sessionSetSpy.restore();
    axios.get = originalAxiosGet;
  });
});
