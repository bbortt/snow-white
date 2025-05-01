/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  '{,**/}*.{java,js,json,md,xml,yaml,yml}': ['prettier --write'],
  'microservices/api-gateway/{,**/}*.{js,cjs,mjs,ts,cts,mts,html,tsx,css,scss}': ['prettier --write --config microservices/api-gateway/prettier.config.js'],
};
