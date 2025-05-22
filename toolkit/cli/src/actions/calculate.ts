import chalk from 'chalk';
import { QualityGateApi, QualityGateReport } from '../clients/quality-gate-api';
import {AxiosResponse} from 'axios';

interface calculateOptions {
  gate:string
  service:string
  api:string
  version:string
  url?:string
}

export const calculate = async(options:calculateOptions)=> {
  try {
    console.log(chalk.blue('🚀 Starting quality gate calculation...'));
    console.log(chalk.gray(`Gate: ${options.gate}`));
    console.log(chalk.gray(`Service: ${options.service}`));
    console.log(chalk.gray(`API: ${options.api}`));
    console.log(chalk.gray(`Version: ${options.version}`));
    console.log(chalk.gray(`Base URL: ${options.url}`));
    console.log('');

    const apiClient = new QualityGateApi(undefined, options.url);

    const calculationRequest = {
      serviceName: options.service,
      apiName: options.api,
      apiVersion: options.version
    };

    const response:AxiosResponse<QualityGateReport> = await apiClient.calculateQualityGate(
      options.gate,
      calculationRequest
    );

    console.log(chalk.green('✅ Quality gate calculation initiated successfully!'));
    console.log('');

    if (response.headers.location) {
      console.log(`Location: ${response.headers.location}`);
    }

    console.log('');
    console.log(chalk.yellow('💡 Use the returned URL to check the calculation report.'));
  } catch (error: unknown) {
    console.error(chalk.red('❌ Failed to trigger quality gate calculation'));
    console.log('');

    // TODO: Need to type-check this!
    if (error.response) {
      console.error(chalk.red(`Status: ${error.response.status}`));
      console.error(chalk.red(`Error: ${error.response.statusText}`));

      if (error.response.data) {
        console.error('Details:');
        console.error(JSON.stringify(error.response.data, null, 2));
      }
    } else if (error.request) {
      console.error(chalk.red('No response received from server'));
      console.error(chalk.gray('Check if the service is running and accessible'));
    } else {
      console.error(chalk.red(`Error: ${error instanceof Error ? error.message: JSON.stringify(error)}`));
    }

    process.exit(1);
  }
}
