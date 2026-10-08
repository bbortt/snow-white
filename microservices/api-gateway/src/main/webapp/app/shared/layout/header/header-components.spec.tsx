/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router';

import { Brand } from './header-components';

describe('header-components', () => {
  describe('Brand', () => {
    afterEach(() => {
      (globalThis as any).VERSION = 'DEV';
    });

    it('should prefix the version with v when it is not already prefixed', () => {
      (globalThis as any).VERSION = '1.2.3';

      render(
        <MemoryRouter>
          <Brand />
        </MemoryRouter>,
      );

      expect(screen.getByText('v1.2.3')).toBeInTheDocument();
    });

    it('should not add another v prefix when the version already starts with v', () => {
      (globalThis as any).VERSION = 'v1.2.3';

      render(
        <MemoryRouter>
          <Brand />
        </MemoryRouter>,
      );

      expect(screen.getByText('v1.2.3')).toBeInTheDocument();
    });
  });
});
