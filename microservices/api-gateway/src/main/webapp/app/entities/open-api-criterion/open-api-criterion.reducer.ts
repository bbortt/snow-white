import axios from 'axios';
import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { IQueryParams, createEntitySlice, EntityState, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { IOpenApiCriterion, defaultValue } from 'app/shared/model/open-api-criterion.model';

const initialState: EntityState<IOpenApiCriterion> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const apiUrl = 'api/rest/v1/criteria/openapi';

// Actions

export const getEntities = createAsyncThunk('openApiCriterion/fetch_entity_list', async ({ page, size, sort }: IQueryParams) => {
  const requestUrl = `${apiUrl}?${sort ? `page=${page}&size=${size}&sort=${sort}&` : ''}cacheBuster=${new Date().getTime()}`;
  return axios.get<IOpenApiCriterion[]>(requestUrl);
});

export const getEntity = createAsyncThunk(
  'openApiCriterion/fetch_entity',
  async (id: string | number) => {
    const requestUrl = `${apiUrl}/${id}`;
    return axios.get<IOpenApiCriterion>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

// slice

export const OpenApiCriterionSlice = createEntitySlice({
  name: 'openApiCriterion',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload.data;
      })
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data, headers } = action.payload;

        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers['x-total-count'], 10),
        };
      })
      .addMatcher(isPending(getEntities, getEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      });
  },
});

export const { reset } = OpenApiCriterionSlice.actions;

// Reducer
export default OpenApiCriterionSlice.reducer;
