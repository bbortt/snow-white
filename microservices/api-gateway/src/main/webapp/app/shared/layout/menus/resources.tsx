/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './resources.scss';

import React from 'react';
import { Translate, translate } from 'react-jhipster';
import { DropdownItem } from 'reactstrap';

import { NavDropdown } from './menu-components';

export const ResourcesMenu = () => (
  <NavDropdown
    icon="th-list"
    name={translate('global.menu.resources.main')}
    id="entity-menu"
    style={{ maxHeight: '80vh', overflow: 'auto' }}
    data-testid="resources-menu"
  >
    <DropdownItem key="swagger-ui">
      <a href="/swagger-ui/index.html" target="_blank" className="dropdown-item link-no-style">
        <div className="img-svg img-svg-openapi svg-inline--fa"></div>
        <Translate contentKey="global.menu.resources.swagger" />
      </a>
    </DropdownItem>
  </NavDropdown>
);
