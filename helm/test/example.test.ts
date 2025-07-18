/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

describe('Helm Chart Rendering', () => {
  it('renders a deployment with the correct image', async () => {
    const manifests = await renderHelmChart({
      chartPath: '../charts/my-chart',
      values: {
        image: {
          repository: 'nginx',
          tag: '1.25',
        },
      },
    });

    const deployment = manifests.find((m) => m.kind === 'Deployment');
    expect(deployment).toBeDefined();

    expect(deployment.spec.template.spec.containers[0].image).toBe(
      'nginx:1.25',
    );
  });
});
