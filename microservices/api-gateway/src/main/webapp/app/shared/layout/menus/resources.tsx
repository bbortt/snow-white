import './resources.scss';

import React from 'react';
import { Translate, translate } from 'react-jhipster';
import { NavDropdown } from './menu-components';
import { DropdownItem } from 'reactstrap';

export const ResourcesMenu = () => (
  <NavDropdown
    icon="th-list"
    name={translate('global.menu.resources.main')}
    id="entity-menu"
    data-cy="entity"
    style={{ maxHeight: '80vh', overflow: 'auto' }}
  >
    <DropdownItem key="swagger-ui">
      <a href="/swagger-ui/index.html" target="_blank" className="dropdown-item link-no-style">
        <div className="img-svg img-svg-openapi svg-inline--fa"></div>
        <Translate contentKey="global.menu.resources.swagger" />
      </a>
    </DropdownItem>
  </NavDropdown>
);
