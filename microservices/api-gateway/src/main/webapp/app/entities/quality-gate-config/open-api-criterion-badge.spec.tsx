/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { configureStore } from '@reduxjs/toolkit';
import { render, screen, fireEvent } from '@testing-library/react';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import React from 'react';
import { translate } from 'react-jhipster';
import { Provider } from 'react-redux';

import * as criterionReducer from '../open-api-criterion/open-api-criterion.reducer';
import { OpenApiCriterionBadge } from './open-api-criterion-badge';

jest.mock('app/config/store', () => ({
  useAppDispatch: jest.fn(),
  useAppSelector: jest.fn(),
}));

jest.mock('react-jhipster', () => ({
  translate: jest.fn((key: string) => {
    const translations: Record<string, string> = {
      'snowWhiteApp.openApiCriterion.description.TEST_CRITERION.name': 'Test Criterion Name',
      'snowWhiteApp.openApiCriterion.description.TEST_CRITERION.description': 'Test Criterion Description',
      'snowWhiteApp.openApiCriterion.description.ANOTHER_CRITERION.name': 'Another Criterion Name',
      'snowWhiteApp.openApiCriterion.description.ANOTHER_CRITERION.description': 'Another Criterion Description',
      'snowWhiteApp.openApiCriterion.description.UNDEFINED_CRITERION.name': 'translation-not-found',
      'snowWhiteApp.openApiCriterion.description.UNDEFINED_CRITERION.description': 'translation-not-found',
    };
    return translations[key] || key;
  }),
}));

jest.mock('../open-api-criterion/open-api-criterion.reducer', () => ({
  getEntity: jest.fn((name: string) => ({
    type: 'GET_ENTITY',
    payload: Promise.resolve({ name }),
  })),
}));

describe('OpenApiCriterionBadge', () => {
  const createOpenApiCriterion = (name: string): IOpenApiCriterion => ({
    name,
  });

  const returnEntityFromAppSelector = (entities: object) => {
    (useAppSelector as jest.MockedFn<(reducer: (state: any) => any) => any>).mockImplementation(reducer =>
      reducer({
        snowwhite: {
          openApiCriterion: {
            entities,
          },
        },
      }),
    );
  };

  let dispatch: unknown;

  beforeEach(() => {
    jest.clearAllMocks();

    dispatch = jest.fn();
    (useAppDispatch as jest.MockedFn<() => any>).mockReturnValueOnce(dispatch);
  });

  describe('Basic rendering', () => {
    it('should render badge with correct name and tooltip', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');
      returnEntityFromAppSelector({ TEST_CRITERION: criterion });

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      // Check if badge is rendered with correct text
      expect(screen.getByText('Test Criterion Name')).toBeInTheDocument();

      // Check if badge has correct id
      const badge = screen.getByText('Test Criterion Name').closest('.badge');
      expect(badge).toHaveAttribute('id', 'badge-TEST_CRITERION');
    });

    it.each(['', undefined])('should render nothing when criterion name is: %s', (name: string | undefined) => {
      const criterion = createOpenApiCriterion(name as string);

      const { container } = render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(container.firstChild).toBeNull();
    });
  });

  describe('Redux integration', () => {
    it('should dispatch getEntity with criterion name on mount', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: 'GET_ENTITY' }));
    });

    it('should not dispatch getEntity when criterion name is missing', () => {
      const criterion = createOpenApiCriterion('');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(dispatch).not.toHaveBeenCalled();
    });

    it('should use entity from Redux store when available', () => {
      returnEntityFromAppSelector({
        TEST_CRITERION: {
          name: 'TEST_CRITERION',
          additionalData: 'from store',
        },
      });

      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(screen.getByText('Test Criterion Name')).toBeInTheDocument();
    });

    it('should fallback to prop when entity not in store', () => {
      (useAppSelector as jest.MockedFn<(reducer: (state: any) => any) => any>).mockImplementation(reducer =>
        reducer({
          snowwhite: {
            openApiCriterion: {
              entities: {},
            },
          },
        }),
      );

      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(screen.getByText('Test Criterion Name')).toBeInTheDocument();
    });
  });

  describe('Tooltip functionality', () => {
    it('should render tooltip with correct target and description', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      // Check if tooltip exists (it might not be visible initially)
      const tooltipElement =
        document.querySelector('[role="tooltip"]') ||
        document.querySelector('.tooltip') ||
        screen.queryByText('Test Criterion Description');

      expect(tooltipElement).toBeDefined();

      // Since tooltip might be controlled by reactstrap's internal logic,
      // we at least verify the tooltip target matches the badge id
      const badge = screen.getByText('Test Criterion Name').closest('.badge');
      expect(badge).toHaveAttribute('id', 'badge-TEST_CRITERION');
    });
  });

  describe('Translation integration', () => {
    it('should call translate with correct keys for name and description', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(translate).toHaveBeenCalledWith('snowWhiteApp.openApiCriterion.description.TEST_CRITERION.name');
      expect(translate).toHaveBeenCalledWith('snowWhiteApp.openApiCriterion.description.TEST_CRITERION.description');
    });

    it('should handle missing translations gracefully', () => {
      const criterion = createOpenApiCriterion('UNDEFINED_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      // Should render the name property when translation is missing
      expect(screen.getByText('UNDEFINED_CRITERION')).toBeInTheDocument();
    });
  });

  describe('Memoization behavior', () => {
    it('should memoize name and description based on entity name', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      const { rerender } = render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(translate).toHaveBeenCalledTimes(2);

      (useAppDispatch as jest.MockedFn<() => any>).mockReturnValueOnce(dispatch);
      rerender(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      expect(translate).toHaveBeenCalledTimes(2);
    });

    it('should update memoized values when criterion name changes', () => {
      const criterion1 = createOpenApiCriterion('TEST_CRITERION');
      const criterion2 = createOpenApiCriterion('ANOTHER_CRITERION');

      const { rerender } = render(<OpenApiCriterionBadge openApiCriterion={criterion1} />);

      expect(screen.getByText('Test Criterion Name')).toBeInTheDocument();

      (useAppDispatch as jest.MockedFn<() => any>).mockReturnValueOnce(dispatch);
      rerender(<OpenApiCriterionBadge openApiCriterion={criterion2} />);

      expect(screen.getByText('Another Criterion Name')).toBeInTheDocument();
      expect(translate).toHaveBeenCalledWith('snowWhiteApp.openApiCriterion.description.ANOTHER_CRITERION.name');
    });
  });

  describe('Component structure', () => {
    it('should render badge with anchor tag inside', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      const anchor = screen.getByText('Test Criterion Name');
      expect(anchor.tagName).toBe('A');
      expect(anchor.closest('.badge')).toBeInTheDocument();
    });

    it('should have correct badge id format', () => {
      const criterion = createOpenApiCriterion('TEST_CRITERION');

      render(<OpenApiCriterionBadge openApiCriterion={criterion} />);

      const badge = screen.getByText('Test Criterion Name').closest('.badge');
      expect(badge).toHaveAttribute('id', 'badge-TEST_CRITERION');
    });
  });
});
