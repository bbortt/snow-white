/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './open-api-criterion-badge.scss';

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import React, { useEffect, useMemo, useState } from 'react';
import { translate } from 'react-jhipster';
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
    if (openApiCriterion.name) {
      dispatch(getEntity(openApiCriterion.name));
    }
  }, [openApiCriterion.name, dispatch]);

  const openApiCriterionEntity = useAppSelector(
    state => state.snowwhite.openApiCriterion.entities?.[openApiCriterion.name!] || openApiCriterion,
  );

  const name = useMemo(() => {
    const translation = translate(`snowWhiteApp.openApiCriterion.description.${openApiCriterionEntity.name}.name`);
    if (translation?.startsWith('translation-not-found')) {
      return openApiCriterionEntity.name;
    }
    return translation;
  }, [openApiCriterionEntity.name]);

  const description = useMemo(() => {
    const translation = translate(`snowWhiteApp.openApiCriterion.description.${openApiCriterionEntity.name}.description`);
    if (translation?.startsWith('translation-not-found')) {
      return openApiCriterionEntity.description;
    }
    return translation;
  }, [openApiCriterionEntity.name]);

  if (!openApiCriterionEntity.name) {
    return null;
  }

  return (
    <>
      <Badge id={`badge-${openApiCriterionEntity.name}`}>
        <a>{name}</a>
      </Badge>
      <Tooltip target={`badge-${openApiCriterionEntity.name}`} isOpen={tooltipOpen} toggle={toggle}>
        {description}
      </Tooltip>
    </>
  );
};

export default OpenApiCriterionBadge;
