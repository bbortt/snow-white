/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import React, { useEffect, useState } from 'react';
import { Translate } from 'react-jhipster';
import { useLocation, useNavigate, useParams } from 'react-router';
import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from 'reactstrap';

import { FILE_OWNED_REASON_ID, QualityGateConfigFileOwned } from './quality-gate-config-file-owned';
import { getEntity, deleteEntity, getGateOwnership } from './quality-gate-config.reducer';

export const QualityGateConfigDeleteDialog = () => {
  const dispatch = useAppDispatch();

  const location = useLocation();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const [loadModal, setLoadModal] = useState(false);

  useEffect(() => {
    dispatch(getEntity(id!));
    // The gate read reports the mode, but not when it 404s — and a deep link to a name the gate
    // file does not define is exactly when this route must still refuse rather than offer a delete.
    void dispatch(getGateOwnership());
    setLoadModal(true);
  }, []);

  const qualityGateConfigEntity: IQualityGateConfig = useAppSelector(state => state.snowwhite.qualityGateConfig.entity);
  const updateSuccess = useAppSelector(state => state.snowwhite.qualityGateConfig.updateSuccess);
  const loading = useAppSelector(state => state.snowwhite.qualityGateConfig.loading);
  const fileOwned: boolean = useAppSelector(state => state.snowwhite.qualityGateConfig.fileOwned);

  const handleClose = () => {
    navigate(`/quality-gate-config${location.search}`);
  };

  useEffect(() => {
    if (updateSuccess && loadModal) {
      handleClose();
      setLoadModal(false);
    }
  }, [updateSuccess]);

  const confirmDelete = () => {
    if (qualityGateConfigEntity.name) {
      dispatch(deleteEntity(qualityGateConfigEntity.name));
    }
  };

  // No dialog until the gate read has reported the ownership mode.
  if (!loadModal || loading) {
    return null;
  }

  // No delete dialog, but still a way back: this route is reachable by deep link, and the reason
  // alone leaves the user on a dead end with nothing but the browser's back button.
  if (fileOwned) {
    return (
      <Modal isOpen toggle={handleClose} aria-labelledby={FILE_OWNED_REASON_ID}>
        <ModalBody>
          <QualityGateConfigFileOwned />
        </ModalBody>
        <ModalFooter>
          <Button color="secondary" onClick={handleClose} data-testid="qualityGateConfigFileOwnedBackButton">
            <FontAwesomeIcon icon="arrow-left" />
            &nbsp;
            <Translate contentKey="entity.action.back">Back</Translate>
          </Button>
        </ModalFooter>
      </Modal>
    );
  }

  return (
    <Modal isOpen toggle={handleClose}>
      <ModalHeader toggle={handleClose} data-testid="qualityGateConfigDeleteDialogHeading">
        <Translate contentKey="entity.delete.title">Confirm delete operation</Translate>
      </ModalHeader>
      <ModalBody id="snowWhiteApp.qualityGateConfig.delete.question">
        <Translate contentKey="snowWhiteApp.qualityGateConfig.delete.question" interpolate={{ id: qualityGateConfigEntity.name }}>
          Are you sure you want to delete this QualityGateConfig?
        </Translate>
      </ModalBody>
      <ModalFooter>
        <Button color="secondary" onClick={handleClose}>
          <FontAwesomeIcon icon="ban" />
          &nbsp;
          <Translate contentKey="entity.action.cancel">Cancel</Translate>
        </Button>
        <Button id="jhi-confirm-delete-qualityGateConfig" data-testid="entityConfirmDeleteButton" color="danger" onClick={confirmDelete}>
          <FontAwesomeIcon icon="trash" />
          &nbsp;
          <Translate contentKey="entity.action.delete">Delete</Translate>
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default QualityGateConfigDeleteDialog;
