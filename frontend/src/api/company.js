import api from './api'; 
export const getCompanyByTicker = (ticker) =>
  api.get(`/companies/${ticker}`);
