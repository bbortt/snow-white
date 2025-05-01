import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

export interface IQualityGateConfig {
  name?: string;
  description?: string | null;
  isPredefined?: boolean;
  openApiCriteria?: IOpenApiCriterion[] | null;
}

export const defaultValue: Readonly<IQualityGateConfig> = {
  isPredefined: false,
};
