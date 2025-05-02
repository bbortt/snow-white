import { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

export interface IOpenApiCriterion {
  name?: string;
  label?: string;
  description?: string | null;
  qualityGateConfigs?: IQualityGateConfig[] | null;
}

export const defaultValue: Readonly<IOpenApiCriterion> = {};
