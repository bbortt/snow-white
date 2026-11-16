/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { QualityGateConfig as QualityGateConfigDto } from 'app/clients/quality-gate-api';
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

import { combineReducers, configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import openApiCriterion from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import axios from 'axios';
import React from 'react';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';

import QualityGateConfigRoutes from './index';
import { FILE_OWNED_REASON_ID } from './quality-gate-config-file-owned';
import qualityGateConfig from './quality-gate-config.reducer';

jest.mock('react-jhipster', () => ({
  ...jest.requireActual('react-jhipster'),
  Translate: ({ contentKey }: { contentKey: string }) => <span>{contentKey}</span>,
  translate: (key: string) => key,
}));

const GATES: QualityGateConfigDto[] = [
  { name: 'builtin', isPredefined: true, minCoveragePercentage: 80, openApiCoverageCriteria: [] },
  { name: 'custom', isPredefined: false, minCoveragePercentage: 50, openApiCoverageCriteria: [] },
];

type Ownership = 'database' | 'file' | 'file-empty';

/**
 * Stubs the gate reads at the HTTP boundary, so the header travels through axios exactly as a server's
 * would, and records every request so a write on a gate path cannot go unnoticed.
 */
const stubServer = (ownership: Ownership, gateReadsHeld?: Promise<void>) => {
  const requests: { method: string; url: string }[] = [];
  const gates = ownership === 'file-empty' ? [] : GATES;

  const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
    const method = config.method!.toUpperCase();
    const url = config.url!;
    requests.push({ method, url });

    const respond = (status: number, data: unknown, headers: Record<string, string> = {}): AxiosResponse =>
      ({ status, statusText: '', data, headers, config, request: {} }) as AxiosResponse;
    const ownershipHeader = { 'x-quality-gate-ownership': ownership };

    if (!url.includes('/quality-gates')) {
      return respond(200, []);
    }
    if (method !== 'GET') {
      return respond(method === 'DELETE' ? 204 : 200, config.data ? JSON.parse(config.data) : null);
    }

    await gateReadsHeld;
    const name = /\/quality-gates\/([^/?]+)/.exec(url)?.[1];
    if (name) {
      return respond(
        200,
        gates.find(gate => gate.name === name),
        ownershipHeader,
      );
    }
    return respond(200, gates, { ...ownershipHeader, 'x-total-count': String(gates.length) });
  };

  axios.defaults.adapter = adapter;
  return {
    requests,
    gateWrites: () => requests.filter(({ method, url }) => method !== 'GET' && url.includes('/quality-gates')),
  };
};

const LocationProbe = () => <div data-testid="location">{useLocation().pathname}</div>;

/** Renders the route directly, as a deep link, bookmark or browser history would reach it. */
const renderAt = (path: string) => {
  const store = configureStore({
    reducer: { snowwhite: combineReducers({ qualityGateConfig, openApiCriterion }) },
    middleware: getDefaultMiddleware => getDefaultMiddleware({ serializableCheck: false }),
  });
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/quality-gate-config/*" element={<QualityGateConfigRoutes />} />
        </Routes>
        <LocationProbe />
      </MemoryRouter>
    </Provider>,
  );
};

const row = async (name: string) => (await screen.findByText(name, { selector: 'a' })).closest('tr')!;

const expectWriteControlDisabled = (control: HTMLElement) => {
  expect(control).toHaveAttribute('aria-disabled', 'true');
  expect(control).toHaveClass('disabled');
  expect(control).toHaveAttribute('aria-describedby', FILE_OWNED_REASON_ID);
  expect(control).toHaveAttribute('title', 'snowWhiteApp.qualityGateConfig.fileOwned');
};

const expectWriteControlEnabled = (control: HTMLElement) => {
  expect(control).not.toHaveAttribute('aria-disabled');
  expect(control).not.toHaveClass('disabled');
  expect(control).not.toHaveAttribute('title');
};

const reason = () => screen.queryAllByTestId('qualityGateConfigFileOwned');

describe('Quality gate screens and the gate ownership mode', () => {
  const defaultAdapter = axios.defaults.adapter;

  afterEach(() => {
    axios.defaults.adapter = defaultAdapter;
  });

  describe('with the gate reads reporting file ownership', () => {
    it('should render the list with every write control disabled and state the reason once', async () => {
      const server = stubServer('file');
      renderAt('/quality-gate-config');

      const custom = await row('custom');
      expect(reason()).toHaveLength(1);
      expect(reason()[0]).toHaveAttribute('id', FILE_OWNED_REASON_ID);
      expectWriteControlDisabled(screen.getByTestId('entityCreateButton'));
      expectWriteControlDisabled(within(custom).getByTestId('entityEditButton'));
      expectWriteControlDisabled(within(custom).getByTestId('entityDeleteButton'));

      // A predefined gate's controls stay omitted, as they are today.
      const builtin = await row('builtin');
      expect(within(builtin).queryByTestId('entityEditButton')).toBeNull();
      expect(within(builtin).queryByTestId('entityDeleteButton')).toBeNull();

      // Reads are not what the mode restricts.
      expect(within(custom).getByTestId('entityDetailsButton')).toHaveAttribute('href', '/quality-gate-config/custom');
      expect(screen.getByTestId('location')).toHaveTextContent(/^\/quality-gate-config$/);

      fireEvent.click(screen.getByTestId('entityCreateButton'));
      fireEvent.click(within(custom).getByTestId('entityEditButton'));
      fireEvent.click(within(custom).getByTestId('entityDeleteButton'));

      expect(screen.getByTestId('location')).toHaveTextContent(/^\/quality-gate-config$/);
      expect(server.gateWrites()).toEqual([]);
    });

    it('should disable create and state the reason on a gate file that defines no gates', async () => {
      stubServer('file-empty');
      renderAt('/quality-gate-config');

      expect(await screen.findByText('snowWhiteApp.qualityGateConfig.home.notFound')).toBeInTheDocument();
      expect(reason()).toHaveLength(1);
      expectWriteControlDisabled(screen.getByTestId('entityCreateButton'));
    });

    it.each<Ownership>(['file', 'file-empty'])(
      'should refuse the new route with the reason on %s, and read the mode for it',
      async ownership => {
        const server = stubServer(ownership);
        renderAt('/quality-gate-config/new');

        expect(await screen.findByTestId('qualityGateConfigFileOwned')).toBeInTheDocument();
        expect(screen.queryByTestId('entityCreateSaveButton')).toBeNull();
        expect(screen.queryByTestId('name')).toBeNull();
        expect(server.requests).toContainEqual({ method: 'GET', url: expect.stringMatching(/\/quality-gates\?(.*&)?size=1(&|$)/) });
        expect(server.gateWrites()).toEqual([]);
      },
    );

    it('should refuse the edit route with the reason', async () => {
      const server = stubServer('file');
      renderAt('/quality-gate-config/custom/edit');

      expect(await screen.findByTestId('qualityGateConfigFileOwned')).toBeInTheDocument();
      expect(screen.queryByTestId('entityCreateSaveButton')).toBeNull();
      expect(screen.queryByTestId('name')).toBeNull();
      expect(server.gateWrites()).toEqual([]);
    });

    it('should refuse the delete route with the reason', async () => {
      const server = stubServer('file');
      renderAt('/quality-gate-config/custom/delete');

      expect(await screen.findByTestId('qualityGateConfigFileOwned')).toBeInTheDocument();
      expect(screen.queryByTestId('qualityGateConfigDeleteDialogHeading')).toBeNull();
      expect(screen.queryByTestId('entityConfirmDeleteButton')).toBeNull();
      expect(server.gateWrites()).toEqual([]);
    });

    it.each([
      ['new', '/quality-gate-config/new', 'entityCreateSaveButton'],
      ['edit', '/quality-gate-config/custom/edit', 'entityCreateSaveButton'],
      ['delete', '/quality-gate-config/custom/delete', 'entityConfirmDeleteButton'],
    ])('should not flash the %s form or dialog before the mode is known', async (_route, path, writeControl) => {
      let releaseGateReads: () => void = () => {};
      stubServer(
        'file',
        new Promise<void>(resolve => {
          releaseGateReads = resolve;
        }),
      );
      renderAt(path);

      expect(screen.queryByTestId(writeControl)).toBeNull();
      expect(reason()).toHaveLength(0);

      releaseGateReads();

      expect(await screen.findByTestId('qualityGateConfigFileOwned')).toBeInTheDocument();
      expect(screen.queryByTestId(writeControl)).toBeNull();
    });
  });

  describe('with the gate reads reporting database ownership', () => {
    it("should render today's list: predefined controls omitted, the others enabled, no reason", async () => {
      stubServer('database');
      renderAt('/quality-gate-config');

      const custom = await row('custom');
      const builtin = await row('builtin');
      expect(reason()).toHaveLength(0);
      expectWriteControlEnabled(screen.getByTestId('entityCreateButton'));
      expectWriteControlEnabled(within(custom).getByTestId('entityEditButton'));
      expectWriteControlEnabled(within(custom).getByTestId('entityDeleteButton'));
      expect(within(builtin).queryByTestId('entityEditButton')).toBeNull();
      expect(within(builtin).queryByTestId('entityDeleteButton')).toBeNull();
    });

    it.each([
      ['entityCreateButton', '/quality-gate-config/new'],
      ['entityEditButton', '/quality-gate-config/custom/edit'],
      ['entityDeleteButton', '/quality-gate-config/custom/delete'],
    ])('should navigate on %s', async (control, target) => {
      stubServer('database');
      renderAt('/quality-gate-config');

      const custom = await row('custom');
      fireEvent.click(within(custom).queryByTestId(control) ?? screen.getByTestId(control));

      expect(screen.getByTestId('location')).toHaveTextContent(target);
    });

    it('should render the new form and POST the gate it submits', async () => {
      const server = stubServer('database');
      renderAt('/quality-gate-config/new');

      fireEvent.change(await screen.findByTestId('name'), { target: { value: 'fresh' } });
      fireEvent.click(screen.getByTestId('entityCreateSaveButton'));

      await waitFor(() => expect(server.gateWrites()).toEqual([{ method: 'POST', url: '/api/rest/v1/quality-gates' }]));
      expect(reason()).toHaveLength(0);
    });

    it('should render the edit form and PUT the gate it submits', async () => {
      const server = stubServer('database');
      renderAt('/quality-gate-config/custom/edit');

      expect(await screen.findByTestId('name')).toHaveValue('custom');
      expect(reason()).toHaveLength(0);
      fireEvent.click(screen.getByTestId('entityCreateSaveButton'));

      await waitFor(() => expect(server.gateWrites()).toEqual([{ method: 'PUT', url: '/api/rest/v1/quality-gates/custom' }]));
    });

    it('should render the delete dialog and DELETE the gate it confirms', async () => {
      const server = stubServer('database');
      renderAt('/quality-gate-config/custom/delete');

      expect(await screen.findByTestId('qualityGateConfigDeleteDialogHeading')).toBeInTheDocument();
      expect(reason()).toHaveLength(0);
      fireEvent.click(screen.getByTestId('entityConfirmDeleteButton'));

      await waitFor(() => expect(server.gateWrites()).toEqual([{ method: 'DELETE', url: '/api/rest/v1/quality-gates/custom' }]));
    });
  });
});
