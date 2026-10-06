/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './api-criterion-info.scss';

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { TextWithCode } from 'app/shared/TextWithCode';
import React, { useEffect, useMemo, useState } from 'react';
import { translate } from 'react-jhipster';
import { Button, Tooltip } from 'reactstrap';

import { getEntity } from '../open-api-criterion/open-api-criterion.reducer';

export interface OpenApiCriterionBadgeProps {
  apiCriterion: IOpenApiCriterion;
}

export const ApiCriterionInfo: React.FC<OpenApiCriterionBadgeProps> = ({ apiCriterion }) => {
  const [tooltipOpen, setTooltipOpen] = useState(false);

  const dispatch = useAppDispatch();

  const toggle = () => {
    setTooltipOpen(!tooltipOpen);
  };

  useEffect(() => {
    if (apiCriterion.name) {
      dispatch(getEntity(apiCriterion.name));
    }
  }, [apiCriterion.name, dispatch]);

  const openApiCriterionEntity: IOpenApiCriterion = useAppSelector(
    state => state.snowwhite.openApiCriterion.entities?.[apiCriterion.name!] || apiCriterion,
  );

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
      <Button className="noHover" id={`info-${openApiCriterionEntity.name}`}>
        <FontAwesomeIcon icon="info-circle" />
      </Button>
      <Tooltip target={`info-${openApiCriterionEntity.name}`} isOpen={tooltipOpen} toggle={toggle}>
        <TextWithCode text={description} />
      </Tooltip>
    </>
  );
};

export default ApiCriterionInfo;
