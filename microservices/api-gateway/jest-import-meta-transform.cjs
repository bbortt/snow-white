/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

'use strict';

const ts = require('typescript');

// Transforms ESM-only packages (e.g. react-router v8) so they can run in Jest's CJS environment.
// Replaces `import.meta` with a safe empty object before TypeScript converts the remaining ESM syntax to CommonJS.
module.exports = {
  process(sourceText) {
    const preprocessed = sourceText.replace(/\bimport\.meta\b/g, '({})');
    const { outputText } = ts.transpileModule(preprocessed, {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2019,
        allowJs: true,
        esModuleInterop: true,
        allowSyntheticDefaultImports: true,
      },
    });
    return { code: outputText };
  },
};
