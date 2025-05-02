import openApiCriterion from 'app/entities/open-api-criterion/open-api-criterion.reducer';
import qualityGateConfig from 'app/entities/quality-gate-config/quality-gate-config.reducer';
import { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import { EntityState } from 'app/shared/reducers/reducer.utils';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

export interface SnowWhiteState {
  openApiCriterion: EntityState<IOpenApiCriterion>;
  qualityGateConfig: EntityState<IQualityGateConfig>;
}

const entitiesReducers = {
  openApiCriterion,
  qualityGateConfig,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default entitiesReducers;
