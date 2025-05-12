/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render } from '@testing-library/react';
import ErrorBoundary from 'app/shared/error/error-boundary';
import React from 'react';

const ErrorComp = () => {
  throw new Error('test');
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
});
