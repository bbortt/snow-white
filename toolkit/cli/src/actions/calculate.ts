import { AxiosError, AxiosResponse } from 'axios';

import chalk from 'chalk';

import { CalculateQualityGate202Response } from '../clients/quality-gate-api';
import { getApiClient } from '../api/quality-gate-api';

interface calculateOptions {
  qualityGate: string;
  serviceName: string;
  apiName: string;
  apiVersion: string;
  url: string;
}

const calculateQualityGate = async (options: calculateOptions): Promise<void> => {
  console.log(chalk.blue('🚀 Starting quality gate calculation...'));
  console.log(chalk.gray(`Gate: ${options.qualityGate}`));
  console.log(chalk.gray(`Service: ${options.serviceName}`));
  console.log(chalk.gray(`API: ${options.apiName}`));
  console.log(chalk.gray(`Version: ${options.apiVersion}`));
  console.log(chalk.gray(`Base URL: ${options.url}`));
  console.log('');

  const apiClient = getApiClient(options.url);

  const calculationRequest = {
    serviceName: options.serviceName,
    apiName: options.apiName,
    apiVersion: options.apiVersion,
  };

  const response: AxiosResponse<CalculateQualityGate202Response> = await apiClient.calculateQualityGate(
    options.qualityGate,
    calculationRequest,
  );

  console.log(chalk.green('✅ Quality gate calculation initiated successfully!'));
  console.log('');

  if (response.headers.location) {
    console.log(`Location: ${response.headers.location}`);
  }

  console.log('');
  console.log(chalk.yellow('💡 Use the returned URL to check the calculation report.'));
};

export const calculate = async (options: calculateOptions) => {
  try {
    await calculateQualityGate(options);
  } catch (error: unknown) {
    console.error(chalk.red('❌ Failed to trigger quality gate calculation!'));
    console.log('');

    if (error instanceof AxiosError && error.response) {
      console.error(chalk.red(`Status: ${error.response.status}`));

      if (error.response.data) {
        console.error(chalk.red(`Details: ${error.response.data.message}`));
      } else {
        console.error(chalk.red(`Error: ${error.response.statusText}`));
      }
    } else if (error instanceof AxiosError && error.request) {
      console.error(chalk.red('No response received from server'));
      console.error(chalk.gray('Check if the service is running and accessible'));
    } else {
      console.error(chalk.red(`Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
    }

    throw new Error('Failed to calculate quality-gate!');
  }
};
