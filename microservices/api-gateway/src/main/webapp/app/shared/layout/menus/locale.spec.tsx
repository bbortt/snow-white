/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import React from 'react';

import { LocaleMenu } from './locale';

describe('LocaleMenu', () => {
  it('should render a dropdown when more than one language is configured', () => {
    render(<LocaleMenu currentLocale="en" onClick={jest.fn()} />);

    expect(screen.getByTestId('locale-menu')).toBeInTheDocument();
  });

  it('should render the dropdown without a name when currentLocale is not set', () => {
    render(<LocaleMenu currentLocale={undefined as unknown as string} onClick={jest.fn()} />);

    expect(screen.getByTestId('locale-menu')).toBeInTheDocument();
  });

  it('should render nothing when only one language is configured', () => {
    jest.resetModules();
    jest.doMock('app/config/translation', () => ({
      languages: { en: { name: 'English' } },
      locales: ['en'],
    }));

    let SingleLanguageLocaleMenu: typeof LocaleMenu;
    jest.isolateModules(() => {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      SingleLanguageLocaleMenu = require('./locale').LocaleMenu;
    });

    const { container } = render(<SingleLanguageLocaleMenu currentLocale="en" onClick={jest.fn()} />);

    expect(container.firstChild).toBeNull();

    jest.dontMock('app/config/translation');
  });
});
