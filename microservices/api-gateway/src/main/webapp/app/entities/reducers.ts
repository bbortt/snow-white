/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';
import type { EntityState } from 'app/shared/reducers/reducer.utils';

import openApiCriterion from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import qualityGateConfig from 'app/entities/quality-gate-config/quality-gate-config.reducer';
import qualityGate from 'app/entities/quality-gate/quality-gate.reducer';

/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

export interface SnowWhiteState {
  openApiCriterion: EntityState<IOpenApiCriterion>;
  qualityGate: EntityState<IQualityGate>;
  qualityGateConfig: EntityState<IQualityGateConfig>;
}

export const getSnowWhiteState = (getState): SnowWhiteState => {
  return (getState() as { snowwhite: SnowWhiteState }).snowwhite;
};

const entitiesReducers = {
  openApiCriterion,
  qualityGate,
  qualityGateConfig,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default entitiesReducers;
