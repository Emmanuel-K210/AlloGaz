import { apiFetch } from './client';
import type { SearchCriteria, SellerResult } from './types';

export function searchSellers(criteria: SearchCriteria) {
  return apiFetch<SellerResult[]>('/search/sellers', { auth: false, query: criteria });
}
