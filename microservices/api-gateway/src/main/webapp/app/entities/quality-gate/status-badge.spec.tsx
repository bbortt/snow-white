/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { IQualityGate } from 'app/shared/model/quality-gate.model';
import React from 'react';
import { translate } from 'react-jhipster';

jest.mock('react-jhipster', () => ({
  translate: jest.fn().mockImplementation((key: string) => {
    const translations: Record<string, string> = {
      'snowWhiteApp.ReportStatus.PASSED': 'Passed',
      'snowWhiteApp.ReportStatus.FAILED': 'Failed',
      'snowWhiteApp.ReportStatus.PENDING': 'Pending',
      'snowWhiteApp.ReportStatus.UNKNOWN': 'Unknown',
    };
    return translations[key] || key;
  }),
}));

describe('StatusBadge', () => {
  const createQualityGate = (status: string): IQualityGate =>
    ({
      status,
      // Add other required properties if needed based on your IQualityGate interface
    }) as IQualityGate;

  describe('PASSED status', () => {
    it('should render success badge with correct text for PASSED status', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Passed');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('badge-success');
    });

    it('should render success badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('Passed');
      expect(badge.closest('.badge')).toHaveClass('badge-success', 'badge-block');
    });
  });

  describe('FAILED status', () => {
    it('should render danger badge with correct text for FAILED status', () => {
      const qualityGate = createQualityGate('FAILED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Failed');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('badge-danger');
    });

    it('should render danger badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('FAILED');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('Failed');
      expect(badge.closest('.badge')).toHaveClass('badge-danger', 'badge-block');
    });
  });

  describe('Other statuses', () => {
    it('should render primary badge for PENDING status', () => {
      const qualityGate = createQualityGate('PENDING');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Pending');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('badge-primary');
    });

    it('should render primary badge for unknown status', () => {
      const qualityGate = createQualityGate('UNKNOWN');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Unknown');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('badge-primary');
    });

    it('should render primary badge with fill class for other statuses when fill is true', () => {
      const qualityGate = createQualityGate('PENDING');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('Pending');
      expect(badge.closest('.badge')).toHaveClass('badge-primary', 'badge-block');
    });
  });

  describe('Props handling', () => {
    it('should not have badge-block class when fill prop is false', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} fill={false} />);

      const badge = screen.getByText('Passed');
      expect(badge.closest('.badge')).not.toHaveClass('badge-block');
    });

    it('should not have badge-block class when fill prop is undefined', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Passed');
      expect(badge.closest('.badge')).not.toHaveClass('badge-block');
    });
  });

  describe('Translation integration', () => {
    it('should call translate with correct key for each status', () => {
      // Test PASSED
      const passedQualityGate = createQualityGate('PASSED');
      render(<StatusBadge qualityGate={passedQualityGate} />);
      expect(translate).toHaveBeenCalledWith('snowWhiteApp.ReportStatus.PASSED');

      // Test FAILED
      const failedQualityGate = createQualityGate('FAILED');
      render(<StatusBadge qualityGate={failedQualityGate} />);
      expect(translate).toHaveBeenCalledWith('snowWhiteApp.ReportStatus.FAILED');

      // Test other status
      const pendingQualityGate = createQualityGate('PENDING');
      render(<StatusBadge qualityGate={pendingQualityGate} />);
      expect(translate).toHaveBeenCalledWith('snowWhiteApp.ReportStatus.PENDING');
    });
  });

  describe('Badge structure', () => {
    it('should render span with leading space inside badge', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const span = screen.getByText(/\s+Passed/);
      expect(span).toBeInTheDocument();
      expect(span.tagName).toBe('SPAN');
    });
  });

  describe('Edge cases', () => {
    it('should handle empty string status', () => {
      const qualityGate = createQualityGate('');

      render(<StatusBadge qualityGate={qualityGate} />);

      // Should fall back to primary badge for unknown status
      const badge = screen.getByRole('generic'); // Badge component renders as generic role
      expect(badge).toHaveClass('badge-primary');
    });

    it('should handle null-like status gracefully', () => {
      const qualityGate = { status: null } as any;

      render(<StatusBadge qualityGate={qualityGate} />);

      // Should fall back to primary badge
      const badge = screen.getByRole('generic');
      expect(badge).toHaveClass('badge-primary');
    });
  });

  describe('Accessibility', () => {
    it('should render badge with proper structure for screen readers', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Passed');
      expect(badge.closest('.badge')).toBeInTheDocument();
      // Badge should be accessible to screen readers
      expect(badge).toBeVisible();
    });
  });
});
