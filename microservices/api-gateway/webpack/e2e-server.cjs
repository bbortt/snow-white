/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

// Serves the dev bundle for the Playwright e2e suite (playwright.config.ts): a one-shot webpack
// build, then a minimal static file server with SPA fallback - not `webpack-dev-server`.
//
// webpack-dev-server (via `webpack serve`/`npm run webapp:dev`) is built for interactive live
// development and turned out to be unreliable for this one-shot, non-interactive, sandbox/CI-
// spawned use case: `dev-middleware` queues the very first request(s) until the initial compile
// finishes, and in this spawn context that first flush would race with `BrowserSyncPlugin`
// (which defaults to opening a local browser tab - nothing to open here) and `thread-loader`'s
// worker pool (`os.cpus().length - 1` workers, teardown not awaited by the dev server's own
// lifecycle), each independently capable of producing an unhandled `AggregateError` a few seconds
// after the server starts responding and leaving the very first navigation against it refused.
// A one-shot `compiler.run()` sidesteps all of that: nothing is left mid-flight when the compile
// promise resolves, and the plain `http` server below has no interactive/live-reload machinery to
// race with in the first place.
//
// `ForkTsCheckerWebpackPlugin` (async type-checking) and `WebpackNotifierPlugin` (desktop toast)
// are dropped too - neither serves a purpose for an automated test run, and type errors are
// already caught by lint/build elsewhere in CI.

const fs = require('fs');
const http = require('http');
const path = require('path');

const webpack = require('webpack');

const configFactory = require('./webpack.dev.cjs');

const PORT = 9060;

const CONTENT_TYPES = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.txt': 'text/plain; charset=utf-8',
  '.webapp': 'application/manifest+json',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
};

async function buildOnce(config) {
  const compiler = webpack(config);
  try {
    const stats = await new Promise((resolve, reject) => {
      compiler.run((error, result) => (error ? reject(error) : resolve(result)));
    });
    if (stats.hasErrors()) {
      throw new Error(stats.toString({ colors: false, errorDetails: true }));
    }
  } finally {
    await new Promise(resolve => compiler.close(resolve));
  }
}

function serveStatic(outputDir) {
  const server = http.createServer((req, res) => {
    const requestPath = decodeURIComponent(req.url.split('?')[0]);
    const candidatePath = path.join(outputDir, requestPath);
    // SPA fallback for anything that isn't an existing file under outputDir - mirrors
    // webpack-dev-server's `historyApiFallback` + "404s fallback to /index.html" behaviour.
    const filePath =
      fs.existsSync(candidatePath) && fs.statSync(candidatePath).isFile() ? candidatePath : path.join(outputDir, 'index.html');

    res.setHeader('content-type', CONTENT_TYPES[path.extname(filePath)] ?? 'application/octet-stream');
    fs.createReadStream(filePath).pipe(res);
  });

  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(PORT, resolve);
  });
}

async function main() {
  const config = await configFactory({ stats: 'minimal' });

  config.plugins = config.plugins.filter(
    plugin => !['ForkTsCheckerWebpackPlugin', 'WebpackNotifierPlugin', 'BrowserSyncPlugin'].includes(plugin.constructor.name),
  );
  // `style-loader` emits `module.hot.accept()` calls whenever HMR is potentially available
  // (devServer.hot: true, see webpack.dev.cjs), regardless of whether HotModuleReplacementPlugin
  // is actually registered. webpack-dev-server normally adds that plugin itself; since this is a
  // one-shot `compiler.run()` with no dev-server involved, it has to be added explicitly or the
  // compile fails with "No template for dependency: ModuleHotAcceptDependency". HMR itself is
  // never exercised - the browser only ever loads the single one-shot output below.
  config.plugins.push(new webpack.HotModuleReplacementPlugin());

  for (const rule of config.module.rules) {
    if (Array.isArray(rule.use)) {
      rule.use = rule.use.filter(use => use.loader !== 'thread-loader');
    }
  }

  await buildOnce(config);
  await serveStatic(config.output.path);
  console.log(`e2e static server listening on http://localhost:${PORT}`);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
