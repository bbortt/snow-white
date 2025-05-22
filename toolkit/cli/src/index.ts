#!/usr/bin/env node

import { Command } from 'commander';
import chalk from 'chalk';
import { calculate } from './actions/calculate';

const program = new Command();

program
  .name('snow-white')
  .description('CLI tool to interact with Snow-White.')
  .version('1.0.0');

program
  .command('info')
  .description('Show system information')
  .action(() => {
    console.log(chalk.blue('System Information:'));
    console.log(`Platform: ${process.platform}`);
    console.log(`Architecture: ${process.arch}`);
    console.log(`Node.js version: ${process.version}`);
  });

program
  .command('calculate')
  .description('Trigger a Quality-Gate calculation')
  .requiredOption('-g, --gate <gateName>', 'Quality-Cate configuration name')
  .requiredOption('-s, --service <serviceName>', 'Service name')
  .requiredOption('-a, --api <apiName>', 'API name')
  .requiredOption('-v, --version <version>', 'API version')
  .option('-u, --url <baseUrl>', 'Base URL for Snow-White', 'http://localhost:8090')
  .option('--timeout <timeout>', 'Request timeout in milliseconds', '10000')
  .action(calculate );

program.parse();
