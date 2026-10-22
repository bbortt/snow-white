/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import ErrorBoundary from 'app/shared/error/error-boundary';
import React from 'react';

const ErrorComp = () => {
  throw new Error('test');
};

const FalsyThrowComp = () => {
  // eslint-disable-next-line @typescript-eslint/only-throw-error
  throw undefined;
};

describe('error component', () => {
  beforeEach(() => {
    // ignore console and jsdom errors
    jest.spyOn((window as any)._virtualConsole, 'emit').mockImplementation(() => false);
    jest.spyOn((window as any).console, 'error').mockImplementation(() => false);
  });

  it('Should throw an error when component is not enclosed in Error Boundary', () => {
    expect(() => render(<ErrorComp />)).toThrow(Error);
  });

  it('Should call Error Boundary componentDidCatch method', () => {
    const spy = jest.spyOn(ErrorBoundary.prototype, 'componentDidCatch');
    render(
      <ErrorBoundary>
        <ErrorComp />
      </ErrorBoundary>,
    );
    expect(spy).toHaveBeenCalled();
  });

  it('Should render error details when DEVELOPMENT is true', () => {
    (globalThis as any).DEVELOPMENT = true;

    render(
      <ErrorBoundary>
        <ErrorComp />
      </ErrorBoundary>,
    );

    expect(screen.getByText(/Error: test/)).toBeInTheDocument();

    (globalThis as any).DEVELOPMENT = false;
  });

  it('Should render error details without throwing when a falsy value is thrown', () => {
    (globalThis as any).DEVELOPMENT = true;

    render(
      <ErrorBoundary>
        <FalsyThrowComp />
      </ErrorBoundary>,
    );

    expect(screen.getByText('An unexpected error has occurred.')).toBeInTheDocument();

    (globalThis as any).DEVELOPMENT = false;
  });
});
