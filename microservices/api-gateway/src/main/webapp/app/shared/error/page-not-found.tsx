/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { Translate } from 'react-jhipster';
import { Alert } from 'reactstrap';

const PageNotFound = () => {
  return (
    <div>
      <Alert color="danger">
        <Translate contentKey="error.http.404">The page does not exist.</Translate>
      </Alert>
    </div>
  );
};

export default PageNotFound;
