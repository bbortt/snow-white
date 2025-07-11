/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  '{,**/}*.{cjs,js,mjs,ts,tsx}': ['eslint --fix'],
  '{,**/}*.{json,md,ts,tsx,xml,yaml,yml}': ['prettier --write --config prettier.config.js'],
};
