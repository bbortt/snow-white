/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { setLocale } from 'app/shared/reducers/locale';
import { Storage } from 'react-jhipster';

import { registerLocale } from './translation';

jest.mock('app/shared/reducers/locale', () => ({
  setLocale: jest.fn(locale => ({ type: 'SET_LOCALE', payload: locale })),
}));

describe('translation', () => {
  describe('registerLocale', () => {
    it('should dispatch setLocale with the locale from session storage', () => {
      jest.spyOn(Storage.session, 'get').mockReturnValue('de');
      const dispatch = jest.fn();

      registerLocale({ dispatch });

      expect(setLocale).toHaveBeenCalledWith('de');
      expect(dispatch).toHaveBeenCalledWith({ type: 'SET_LOCALE', payload: 'de' });
    });
  });
});
