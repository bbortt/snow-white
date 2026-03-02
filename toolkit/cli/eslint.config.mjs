/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

// @ts-check

import eslintCommentsPlugin from '@eslint-community/eslint-plugin-eslint-comments/configs';
import { fixupPluginRules } from '@eslint/compat';
import eslint from '@eslint/js';
import vitestPlugin from '@vitest/eslint-plugin';
import eslintPluginPlugin from 'eslint-plugin-eslint-plugin';
import importPlugin from 'eslint-plugin-import';
import jsdocPlugin from 'eslint-plugin-jsdoc';
import nPlugin from 'eslint-plugin-n';
import perfectionistPlugin from 'eslint-plugin-perfectionist';
import regexpPlugin from 'eslint-plugin-regexp';
import unicornPlugin from 'eslint-plugin-unicorn';
import { defineConfig } from 'eslint/config';
import globals from 'globals';
import url from 'node:url';
import tseslint from 'typescript-eslint';

const __dirname = url.fileURLToPath(new URL('.', import.meta.url));

const restrictNamedDeclarations = {
  message: 'Prefer a named export (e.g. `export const ...`) over an object export (e.g. `export { ... }`).',
  selector: 'ExportNamedDeclaration[declaration=null][source=null]',
};

export default defineConfig(
  // register all of the plugins up-front
  {
    name: 'register-all-plugins',
    // note - intentionally uses computed syntax to make it easy to sort the keys
    /* eslint-disable no-useless-computed-key */
    plugins: {
      ['@typescript-eslint']: tseslint.plugin,
      ['eslint-plugin']: eslintPluginPlugin,
      ['import']: fixupPluginRules(importPlugin),
      ['jsdoc']: jsdocPlugin,
      ['n']: nPlugin,
      ['perfectionist']: perfectionistPlugin,
      ['regexp']: regexpPlugin,
      ['unicorn']: unicornPlugin,
      ['vitest']: vitestPlugin,
    },
    /* eslint-enable no-useless-computed-key */
    settings: {
      perfectionist: {
        order: 'asc',
        partitionByComment: true,
        type: 'natural',
      },
    },
  },
  {
    // config with just ignores is the replacement for `.eslintignore`
    ignores: ['**/node_modules/**', '**/target/**', '**/src/clients/**', 'eslint.config.mjs', 'lint-staged.config.cjs'],
    name: 'global-ignores',
  },

  // extends ...
  eslintCommentsPlugin.recommended,
  { name: `${eslint.meta.name}/recommended`, ...eslint.configs.recommended },
  tseslint.configs.strictTypeChecked,
  tseslint.configs.stylisticTypeChecked,
  jsdocPlugin.configs['flat/recommended-typescript-error'],

  // base config
  {
    languageOptions: {
      globals: {
        ...globals.es2020,
        ...globals.node,
      },
      parserOptions: {
        allowAutomaticSingleRunInference: true,
        project: ['./tsconfig.lint.json'],
        tsconfigRootDir: __dirname,
        warnOnUnsupportedTypeScriptVersion: false,
      },
    },
    linterOptions: { reportUnusedDisableDirectives: 'error' },
    name: 'base-config',
    rules: {
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
      '@typescript-eslint/consistent-type-imports': ['error', { disallowTypeAnnotations: true, prefer: 'type-imports' }],
      '@typescript-eslint/explicit-module-boundary-types': 'error',
      '@typescript-eslint/no-confusing-void-expression': ['error', { ignoreVoidReturningFunctions: true }],
      '@typescript-eslint/no-explicit-any': 'error',
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
          enableAutofixRemoval: { imports: true },
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
      '@typescript-eslint/unbound-method': 'off',
      'no-constant-condition': 'off',
      'no-restricted-syntax': ['error', restrictNamedDeclarations],

      //
      // eslint-base
      //

      curly: ['error', 'all'],
      eqeqeq: [
        'error',
        'always',
        {
          null: 'never',
        },
      ],
      'logical-assignment-operators': 'error',
      'no-console': 'off',
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
      'no-unassigned-vars': 'error',
      'no-unreachable-loop': 'error',
      'no-useless-assignment': 'error',
      'no-useless-call': 'error',
      'no-useless-computed-key': 'error',
      'no-useless-concat': 'error',
      'no-useless-rename': 'error',
      'no-var': 'error',
      'no-void': ['error', { allowAsStatement: true }],
      'object-shorthand': 'error',
      'one-var': ['error', 'never'],
      'operator-assignment': 'error',
      'prefer-arrow-callback': 'error',
      'prefer-const': 'error',
      'prefer-object-has-own': 'error',
      'prefer-object-spread': 'error',
      'prefer-rest-params': 'error',
      'prefer-template': 'error',
      radix: 'error',

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

      'regexp/no-dupe-disjunctions': 'error',
      'regexp/no-missing-g-flag': 'error',
      'regexp/no-useless-character-class': 'error',
      'regexp/no-useless-flag': 'error',
      'regexp/no-useless-lazy': 'error',
      'regexp/no-useless-non-capturing-group': 'error',
      'regexp/prefer-quantifier': 'error',
      'regexp/prefer-question-quantifier': 'error',
      'regexp/prefer-w': 'error',

      //
      // eslint-plugin-n
      //
      'n/no-extraneous-import': 'error',

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
    extends: [tseslint.configs.disableTypeChecked],
    files: ['**/*.js'],
    name: 'js-files-only',
    rules: {
      // turn off other type-aware rules
      '@typescript-eslint/internal/no-poorly-typed-ts-props': 'off',

      // turn off rules that don't apply to JS code
      '@typescript-eslint/explicit-function-return-type': 'off',
    },
  },

  //
  // test file linting
  //

  // test file specific configuration
  {
    extends: [
      vitestPlugin.configs.env,
      {
        rules: {
          '@typescript-eslint/no-empty-function': ['error', { allow: ['arrowFunctions'] }],
          '@typescript-eslint/no-explicit-any': 'off',
          '@typescript-eslint/no-non-null-assertion': 'off',
          '@typescript-eslint/no-unsafe-argument': 'off',
          '@typescript-eslint/no-unsafe-assignment': 'off',
          '@typescript-eslint/no-unsafe-call': 'off',
          '@typescript-eslint/no-unsafe-member-access': 'off',
          '@typescript-eslint/no-unsafe-return': 'off',
          'vitest/hoisted-apis-on-top': 'error',
          'vitest/no-alias-methods': 'error',
          'vitest/no-disabled-tests': 'error',
          'vitest/no-focused-tests': 'error',
          'vitest/no-identical-title': 'error',
          'vitest/no-test-prefixes': 'error',
          'vitest/no-test-return-statement': 'error',
          'vitest/prefer-describe-function-title': 'error',
          'vitest/prefer-each': 'error',
          'vitest/prefer-spy-on': 'error',
          'vitest/prefer-to-be': 'error',
          'vitest/prefer-to-contain': 'error',
          'vitest/prefer-to-have-length': 'error',
          'vitest/valid-expect': 'error',
        },
        settings: { vitest: { typecheck: true } },
      },
    ],

    files: ['src/**/*.spec.ts'],
  },

  //
  // tools and tests
  //
  {
    files: ['eslint.config.mjs', 'src/index.ts'],
    name: 'no-default-export',
    rules: {
      // requirement
      'import/no-default-export': 'off',
    },
  },

  {
    files: ['**/*'],
    name: 'all-files',
    rules: {
      '@typescript-eslint/sort-type-constituents': 'off',
      'perfectionist/sort-classes': [
        'error',
        {
          groups: [
            'index-signature',
            'static-property',
            'static-block',
            ['protected-property', 'protected-accessor-property'],
            ['private-property', 'private-accessor-property'],
            ['property', 'accessor-property'],
            'constructor',
            'static-method',
            'protected-method',
            'private-method',
            'method',
            ['get-method', 'set-method'],
            'unknown',
          ],
        },
      ],
      'perfectionist/sort-enums': 'off',
      'perfectionist/sort-objects': 'error',
      'perfectionist/sort-union-types': [
        'error',
        {
          groups: ['keyword', 'unknown', 'nullish'],
          type: 'natural',
        },
      ],
    },
  },
);
