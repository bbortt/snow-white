/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';

import { serializeAxiosError } from './reducer.utils';

const initialState = {
  ribbonEnv: '',
  inProduction: true,
  isOpenAPIEnabled: false,
};

export type ApplicationProfileState = Readonly<typeof initialState>;

export const getProfile = createAsyncThunk('applicationProfile/get_profile', async () => axios.get('management/info'), {
  serializeError: serializeAxiosError,
});

export const ApplicationProfileSlice = createSlice({
  name: 'applicationProfile',
  initialState: initialState as ApplicationProfileState,
  reducers: {},
  extraReducers(builder) {
    builder.addCase(getProfile.fulfilled, (state, action) => {
      // @ts-expect-error TS2339: Property data does not exist on type RejectWithValue<unknown, unknown>
      const { data } = action.payload;
      state.ribbonEnv = data['display-ribbon-on-profiles'];
      state.inProduction = data.activeProfiles.includes('prod');
      state.isOpenAPIEnabled = data.activeProfiles.includes('api-docs');
    });
  },
});

// Reducer
export default ApplicationProfileSlice.reducer;
