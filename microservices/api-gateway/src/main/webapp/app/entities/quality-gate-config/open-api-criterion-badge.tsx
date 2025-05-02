import './open-api-criterion-badge.scss';

import React, { useEffect, useState } from 'react';
import { Badge, Tooltip } from 'reactstrap';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from '../open-api-criterion/open-api-criterion.reducer';
import { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

export interface OpenApiCriterionBadgeProps {
  openApiCriterion: IOpenApiCriterion;
}

export const OpenApiCriterionBadge: React.FC<OpenApiCriterionBadgeProps> = ({ openApiCriterion }) => {
  const [tooltipOpen, setTooltipOpen] = useState(false);

  const dispatch = useAppDispatch();

  const toggle = () => setTooltipOpen(!tooltipOpen);

  useEffect(() => {
    dispatch(getEntity(openApiCriterion.name));
  }, []);

  const openApiCriterionEntity = useAppSelector(state => state.snowwhite.openApiCriterion.entity);

  return (
    <>
      <Badge id={`badge-${openApiCriterionEntity.name}`}>
        <a>{openApiCriterionEntity.name}</a>
      </Badge>
      <Tooltip target={`badge-${openApiCriterionEntity.name}`} isOpen={tooltipOpen} toggle={toggle}>
        {openApiCriterionEntity.description}
      </Tooltip>
    </>
  );
};

export default OpenApiCriterionBadge;
