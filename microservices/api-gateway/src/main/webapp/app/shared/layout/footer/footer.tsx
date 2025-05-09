/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the PolyForm Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { Translate } from 'react-jhipster';
import { Col, Row } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <div className="footer page-content">
      <Row className="py-3">
        <Col md="4" className="text-center text-md-start mb-2 mb-md-0">
          <h5>Snow-White</h5>
          <p className="small text-muted mb-0">&copy; {currentYear} Timon Borter. All rights reserved.</p>
          <p className="small text-muted">Licensed under PolyForm Small Business License 1.0.0</p>
        </Col>
        <Col md="4" className="text-center mb-2 mb-md-0">
          <h5>Links</h5>
          <ul className="list-unstyled">
            <li>
              <a href="https://github.com/bbortt/snow-white" target="_blank" rel="noopener noreferrer">
                <FontAwesomeIcon icon="github" />
                Source Code
              </a>
            </li>
            <li>
              <a href="https://github.com/bbortt/snow-white/issues" target="_blank" rel="noopener noreferrer">
                <FontAwesomeIcon icon="bug" />
                Report Issues
              </a>
            </li>
          </ul>
        </Col>
        <Col md="4" className="text-center text-md-end">
          <h5>Contact</h5>
          <p className="small mb-0">
            <a href="mailto:timon.borter@gmx.ch">timon.borter@gmx.ch</a>
          </p>
          <p className="small text-muted">
            <Translate contentKey="footer.version">Version</Translate>:
            <span className="ms-1">{VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`}</span>
          </p>
        </Col>
      </Row>
    </div>
  );
};

export default Footer;
