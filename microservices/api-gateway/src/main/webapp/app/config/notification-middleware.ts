/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { FieldErrorVM } from 'app/shared/jhipster/problem-details';

import { getMessageFromHeaders } from 'app/shared/jhipster/headers';
import { isProblemWithMessage } from 'app/shared/jhipster/problem-details';
import { setPerformanceImpacted } from 'app/shared/reducers/application-profile';
import { isFulfilledAction, isRejectedAction } from 'app/shared/reducers/reducer.utils';
import { isAxiosError } from 'axios';
import { translate } from 'react-jhipster';
import { toast } from 'react-toastify';

interface ToastMessage {
  message?: string;
  key?: string;
  data?: any;
}

const addErrorAlert = (message: ToastMessage) => {
  toast.error(message.key ? (translate(message.key, message.data) ?? message.message) : message.message);
};

const getFieldErrorsToasts = (fieldErrors: FieldErrorVM[]): ToastMessage[] =>
  fieldErrors.map(fieldError => {
    if (['Min', 'Max', 'DecimalMin', 'DecimalMax'].includes(fieldError.message)) {
      fieldError.message = 'Size';
    }
    // convert 'something[14].other[4].id' to 'something[].other[].id' so translations can be written to it
    const convertedField = fieldError.field.replaceAll(/\[\d*\]/g, '[]');
    const fieldName = translate(`snowWhiteApp.${fieldError.objectName}.${convertedField}`);
    return { message: `Error on field "${fieldName}"`, key: `error.${fieldError.message}`, data: { fieldName } };
  });

export default store => next => action => {
  const { dispatch } = store;
  const { error, payload } = action;

  /**
   *
   * The notification middleware serves to add success and error notifications
   */
  if (isFulfilledAction(action) && payload?.headers) {
    const { alert, param } = getMessageFromHeaders(payload.headers);
    if (alert) {
      toast.success(translate(alert, { param }));
    }
    dispatch(setPerformanceImpacted(false));
  }

  if (isRejectedAction(action) && isAxiosError(error)) {
    if (error.response) {
      const { response } = error;
      if (response.status === 0) {
        // connection refused, server not reachable
        addErrorAlert({
          message: 'Server not reachable',
          key: 'error.server.not.reachable',
        });
      } else if (response.status === 404) {
        addErrorAlert({
          message: 'Not found',
          key: 'error.url.not.found',
        });
      } else {
        const { data } = response;
        const problem = isProblemWithMessage(data) ? data : null;
        if (problem?.fieldErrors) {
          getFieldErrorsToasts(problem.fieldErrors).forEach(message => addErrorAlert(message));
        } else {
          const { error: toastError, param } = getMessageFromHeaders((response.headers as any) ?? {});
          if (toastError === 'DOWNSTREAM_UNAVAILABLE') {
            dispatch(setPerformanceImpacted(true));
          } else if (toastError) {
            const entityName = translate(`global.menu.entities.${param}`);
            addErrorAlert({ key: toastError, data: { entityName } });
          } else if (problem?.message) {
            addErrorAlert({ message: problem.detail, key: problem.message });
          } else if (typeof data === 'string' && data !== '') {
            addErrorAlert({ message: data });
          } else {
            toast.error(data?.detail ?? data?.message ?? data?.error ?? data?.title ?? 'Unknown error!');
          }
        }
      }
    } else {
      addErrorAlert({ message: error.message ?? 'Unknown error!' });
    }
  } else if (error) {
    addErrorAlert({ message: error.message ?? 'Unknown error!' });
  }

  return next(action);
};
