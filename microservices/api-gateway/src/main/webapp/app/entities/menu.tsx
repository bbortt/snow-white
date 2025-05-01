import React from 'react';
import { Translate } from 'react-jhipster';

import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  return (
    <>
      {/* prettier-ignore */}
      <MenuItem icon="asterisk" to="/open-api-criterion">
        <Translate contentKey="global.menu.entities.openApiCriterion" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/quality-gate-config">
        <Translate contentKey="global.menu.entities.qualityGateConfig" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
