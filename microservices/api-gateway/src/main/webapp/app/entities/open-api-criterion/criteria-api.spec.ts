import { CriteriaApi } from 'app/clients/quality-gate-api';
import { criteriaApi } from 'app/entities/open-api-criterion/criteria-api';

jest.mock('app/clients/quality-gate-api', () => ({
  CriteriaApi: jest.fn(),
}));

describe('Quality-Gate API', () => {
  it('should be defined', () => {
    expect(criteriaApi).toBeDefined();
  });

  it('should be constructed', () => {
    expect(CriteriaApi).toHaveBeenCalledWith(null, SERVER_API_URL);
  });
});
