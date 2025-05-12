/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './open-api-criterion-badge.scss';

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import React, { useEffect, useState } from 'react';
import { Badge, Tooltip } from 'reactstrap';

import { getEntity } from '../open-api-criterion/open-api-criterion.reducer';

export interface OpenApiCriterionBadgeProps {
  openApiCriterion: IOpenApiCriterion;
}

export const OpenApiCriterionBadge: React.FC<OpenApiCriterionBadgeProps> = ({ openApiCriterion }) => {
  const [tooltipOpen, setTooltipOpen] = useState(false);

  const dispatch = useAppDispatch();

  const toggle = () => {
    setTooltipOpen(!tooltipOpen);
  };

  useEffect(() => {
    dispatch(getEntity(openApiCriterion.name!));
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
