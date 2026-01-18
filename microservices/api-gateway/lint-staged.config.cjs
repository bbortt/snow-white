/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

const pattern = '{,**/}*.{ts,tsx,css,scss}';

module.exports = {
  [pattern]: ['eslint --fix'],
  [pattern]: ['prettier --write --config prettier.config.js'],
};
