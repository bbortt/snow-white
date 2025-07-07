/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

// @ts-check

import globals from 'globals';
import prettier from 'eslint-plugin-prettier/recommended';
import tseslint from 'typescript-eslint';
import eslint from '@eslint/js';
import react from 'eslint-plugin-react/configs/recommended.js';
import perfectionistPlugin from 'eslint-plugin-perfectionist';

export default tseslint.config(
  // register all of the plugins up-front
  {
    // note - intentionally uses computed syntax to make it easy to sort the keys

    plugins: {
      ['@typescript-eslint']: tseslint.plugin,
      ['perfectionist']: perfectionistPlugin,
    },

    settings: {
      perfectionist: {
        order: 'asc',
        partitionByComment: true,
        type: 'natural',
      },
    },
  },

  // language options
  {
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },

  // config with just ignores is the replacement for `.eslintignore`
  {
    ignores: [
      '**/node_modules/**',
      '**/dist/**',
      '**/coverage/**',
      '**/__snapshots__/**',
      '**/build/**',
      '**/target/**',
      // Files copied as part of the build
      'src/main/webapp/app/clients/**',
      // Docker files
      'src/main/docker/**',
    ],
  },

  // extends ...
  eslint.configs.recommended,

  {
    files: ['**/*.{js,cjs,mjs}'],
    rules: {
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    },
  },

  {
    files: ['src/main/webapp/**/*.{ts,tsx}'],
    extends: [...tseslint.configs.recommendedTypeChecked, react],
    settings: {
      react: {
        version: 'detect',
      },
    },
    languageOptions: {
      globals: {
        ...globals.browser,
      },
      parserOptions: {
        project: ['./tsconfig.json', './tsconfig.test.json'],
      },
    },
    rules: {
      '@typescript-eslint/member-ordering': [
        'error',
        {
          default: ['static-field', 'instance-field', 'constructor', 'static-method', 'instance-method'],
        },
      ],
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/explicit-member-accessibility': 'off',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/restrict-template-expressions': 'off',
      '@typescript-eslint/restrict-plus-operands': 'off',
      '@typescript-eslint/no-floating-promises': 'off',
      '@typescript-eslint/interface-name-prefix': 'off',
      '@typescript-eslint/no-empty-function': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/array-type': 'off',
      '@typescript-eslint/no-misused-promises': 'off',
      '@typescript-eslint/no-shadow': 'error',
      'spaced-comment': ['warn', 'always'],
      'guard-for-in': 'error',
      'no-labels': 'error',
      'no-caller': 'error',
      'no-bitwise': 'error',
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'no-new-wrappers': 'error',
      'no-eval': 'error',
      'no-new': 'error',
      'no-var': 'error',
      radix: 'error',
      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'prefer-const': 'error',
      'object-shorthand': ['error', 'always', { avoidExplicitReturnArrows: true }],
      'default-case': 'error',
      complexity: ['warn', 40],
      'no-invalid-this': 'off',
      'react/prop-types': 'off',
      'react/display-name': 'off',

      // enforce a sort order across the codebase
      'perfectionist/sort-imports': 'error',
    },
  },

  {
    files: ['src/main/webapp/**/*.spec.ts'],
    rules: {
      '@typescript-eslint/no-empty-function': 'off',
    },
  },

  prettier,
);
