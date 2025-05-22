/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

// Based on: https://github.com/typescript-eslint/typescript-eslint/blob/main/eslint.config.mjs

// @ts-check

import eslintCommentsPlugin from '@eslint-community/eslint-plugin-eslint-comments/configs';
import eslint from '@eslint/js';
import importPlugin from 'eslint-plugin-import';
import jsdocPlugin from 'eslint-plugin-jsdoc';
import perfectionistPlugin from 'eslint-plugin-perfectionist';
import prettier from 'eslint-plugin-prettier/recommended';
import unicornPlugin from 'eslint-plugin-unicorn';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  // register all of the plugins up-front
  {
    // note - intentionally uses computed syntax to make it easy to sort the keys

    plugins: {
      '@typescript-eslint': tseslint.plugin,
      import: importPlugin,
      jsdoc: jsdocPlugin,
      perfectionist: perfectionistPlugin,
      unicorn: unicornPlugin,
    },

    settings: {
      perfectionist: {
        order: 'asc',
        partitionByComment: true,
        type: 'natural',
      },
    },
  },

  // config with just ignores is the replacement for `.eslintignore`
  {
    ignores: [
      '**/__files/**',
      '**/mappings/**',
      '**/node_modules/**',
      '**/dist/**',
      '**/coverage/**',
      '**/__snapshots__/**',
      '**/build/**',
      '**/target/**',
      // Files copied as part of the build
      'src/clients/**',
    ],
  },

  // extends ...
  eslintCommentsPlugin.recommended,
  eslint.configs.recommended,
  tseslint.configs.strictTypeChecked,
  tseslint.configs.stylisticTypeChecked,

  // base config
  {
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
    rules: {
      //
      // jhipster default settings
      //

      '@typescript-eslint/member-ordering': [
        'error',
        {
          default: ['static-field', 'instance-field', 'constructor', 'static-method', 'instance-method'],
        },
      ],
      '@typescript-eslint/explicit-member-accessibility': 'off',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
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

      //
      // typescript-eslint
      //

      '@typescript-eslint/ban-ts-comment': [
        'error',
        {
          minimumDescriptionLength: 5,
          'ts-check': false,
          'ts-expect-error': 'allow-with-description',
          'ts-ignore': true,
          'ts-nocheck': true,
        },
      ],
      '@typescript-eslint/no-confusing-void-expression': ['error', { ignoreVoidReturningFunctions: true }],
      '@typescript-eslint/consistent-type-exports': ['error', { fixMixedExportsWithInlineTypeSpecifier: true }],
      '@typescript-eslint/consistent-type-imports': ['error', { disallowTypeAnnotations: true, prefer: 'type-imports' }],
      '@typescript-eslint/no-require-imports': [
        'error',
        {
          allow: ['/package\\.json$'],
        },
      ],
      '@typescript-eslint/no-unnecessary-condition': ['error', { allowConstantLoopConditions: true, checkTypePredicates: true }],
      '@typescript-eslint/no-unnecessary-type-conversion': 'error',
      '@typescript-eslint/no-unnecessary-type-parameters': 'error',
      '@typescript-eslint/no-unused-expressions': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          caughtErrors: 'all',
          varsIgnorePattern: '^_',
        },
      ],
      '@typescript-eslint/no-var-requires': 'off',
      '@typescript-eslint/prefer-literal-enum-member': [
        'error',
        {
          allowBitwiseExpressions: true,
        },
      ],
      '@typescript-eslint/prefer-nullish-coalescing': [
        'error',
        {
          ignoreConditionalTests: true,
          ignorePrimitives: true,
        },
      ],
      '@typescript-eslint/prefer-string-starts-ends-with': [
        'error',
        {
          allowSingleElementEquality: 'always',
        },
      ],
      '@typescript-eslint/restrict-template-expressions': [
        'error',
        {
          allowAny: true,
          allowBoolean: true,
          allowNullish: true,
          allowNumber: true,
          allowRegExp: true,
        },
      ],
      'no-constant-condition': 'off',

      //
      // eslint-base
      //

      curly: ['error', 'all'],
      'logical-assignment-operators': 'error',
      'no-else-return': [
        'error',
        {
          allowElseIf: false,
        },
      ],
      'no-fallthrough': ['error', { commentPattern: '.*intentional fallthrough.*' }],
      'no-implicit-coercion': ['error', { boolean: false }],
      'no-lonely-if': 'error',
      'no-mixed-operators': 'error',
      'no-process-exit': 'error',
      'no-unreachable-loop': 'error',
      'no-useless-call': 'error',
      'no-useless-computed-key': 'error',
      'no-useless-concat': 'error',
      'no-void': ['error', { allowAsStatement: true }],
      'one-var': ['error', 'never'],
      'operator-assignment': 'error',
      'prefer-arrow-callback': 'error',
      'prefer-object-has-own': 'error',
      'prefer-object-spread': 'error',
      'prefer-rest-params': 'error',
      'prefer-template': 'error',

      //
      // eslint-plugin-eslint-comment
      //

      '@eslint-community/eslint-comments/disable-enable-pair': ['error', { allowWholeFile: true }],

      //
      // eslint-plugin-import
      //

      // enforces consistent type specifier style for named imports
      'import/consistent-type-specifier-style': 'error',
      // disallow non-import statements appearing before import statements
      'import/first': 'error',
      // Require a newline after the last import/require in a group
      'import/newline-after-import': 'error',
      // Forbid import of modules using absolute paths
      'import/no-absolute-path': 'error',
      // disallow AMD require/define
      'import/no-amd': 'error',
      // forbid default exports - we want to standardize on named exports so that imported names are consistent
      'import/no-default-export': 'error',
      // disallow imports from duplicate paths
      'import/no-duplicates': 'error',
      // Forbid the use of extraneous packages
      'import/no-extraneous-dependencies': [
        'error',
        {
          devDependencies: true,
          optionalDependencies: false,
          peerDependencies: true,
        },
      ],
      // Forbid mutable exports
      'import/no-mutable-exports': 'error',
      // Prevent importing the default as if it were named
      'import/no-named-default': 'error',
      // Prohibit named exports
      'import/no-named-export': 'off', // we want everything to be a named export
      // Forbid a module from importing itself
      'import/no-self-import': 'error',
      // Require modules with a single export to use a default export
      'import/prefer-default-export': 'off', // we want everything to be named

      // enforce a sort order across the codebase
      'perfectionist/sort-imports': 'error',

      //
      // eslint-plugin-jsdoc
      //

      // We often use @remarks or other ad-hoc tag names
      'jsdoc/check-tag-names': 'off',
      // https://github.com/gajus/eslint-plugin-jsdoc/issues/1169
      'jsdoc/check-param-names': 'off',
      'jsdoc/informative-docs': 'error',
      // https://github.com/gajus/eslint-plugin-jsdoc/issues/1175
      'jsdoc/require-jsdoc': 'off',
      'jsdoc/require-param': 'off',
      'jsdoc/require-returns': 'off',
      'jsdoc/require-yields': 'off',
      'jsdoc/tag-lines': 'off',

      //
      // eslint-plugin-unicorn
      //

      'unicorn/no-length-as-slice-end': 'error',
      'unicorn/no-lonely-if': 'error',
      'unicorn/no-single-promise-in-promise-methods': 'error',
      'unicorn/no-typeof-undefined': 'error',
      'unicorn/no-useless-spread': 'error',
      'unicorn/prefer-array-some': 'error',
      'unicorn/prefer-export-from': 'error',
      'unicorn/prefer-node-protocol': 'error',
      'unicorn/prefer-regexp-test': 'error',
      'unicorn/prefer-spread': 'error',
      'unicorn/prefer-string-replace-all': 'error',
      'unicorn/prefer-structured-clone': 'error',
    },
  },

  {
    files: ['src/**/*.ts'],
    extends: [...tseslint.configs.recommendedTypeChecked],
    languageOptions: {
      globals: {
        ...globals.node,
      },
      parserOptions: {
        parserOptions: {
          project: ['./tsconfig.json', './tsconfig.test.json'],
        },
      },
    },
  },

  //
  // test file linting
  //

  {
    files: ['src/**/*.spec.ts'],
    rules: {
      '@typescript-eslint/no-empty-function': ['error', { allow: ['arrowFunctions'] }],
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
    },
  },

  //
  // tools and tests
  //

  {
    extends: [tseslint.configs.disableTypeChecked],
    files: ['**/*.{cjs,js,mjs}'],
    rules: {
      // turn off other type-aware rules
      '@typescript-eslint/internal/no-poorly-typed-ts-props': 'off',

      // turn off rules that don't apply to JS code
      '@typescript-eslint/explicit-function-return-type': 'off',
    },
  },

  {
    files: ['eslint.config.mjs'],
    rules: {
      // requirement
      'import/no-default-export': 'off',
    },
  },

  prettier,
);
