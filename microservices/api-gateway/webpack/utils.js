/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

const path = require('node:path');

const tsconfig = require('../tsconfig.json');

module.exports = {
  root,
  mapTypescriptAliasToWebpackAlias,
};

const _root = path.resolve(__dirname, '..');

function root(args) {
  args = Array.prototype.slice.call(arguments, 0);
  return path.join.apply(path, [_root].concat(args));
}

function mapTypescriptAliasToWebpackAlias(alias = {}) {
  const webpackAliases = { ...alias };
  if (!tsconfig.compilerOptions.paths) {
    return webpackAliases;
  }
  Object.entries(tsconfig.compilerOptions.paths)
    .filter(([_key, value]) => {
      // use Typescript alias in Webpack only if this has value
      return !!value.length;
    })
    .map(([key, value]) => {
      // if Typescript alias ends with /* then remove this for Webpack
      const regexToReplace = /\/\*$/;
      const aliasKey = key.replace(regexToReplace, '');
      const aliasValue = value[0].replace(regexToReplace, '');
      return [aliasKey, root(aliasValue)];
    })
    .reduce((aliases, [key, value]) => {
      aliases[key] = value;
      return aliases;
    }, webpackAliases);
  return webpackAliases;
}
