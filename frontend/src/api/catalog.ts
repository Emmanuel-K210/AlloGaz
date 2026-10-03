import { apiFetch } from './client';
import type { CategoryView, ProductView, PublicSeller } from './types';

export function categories() {
  return apiFetch<CategoryView[]>('/catalog/categories', { auth: false });
}

export type ProductFilters = {
  category?: string;
  brand?: string;
  company?: string;
  color?: string;
  sizeKg?: number;
};

export function products(filters: ProductFilters = {}) {
  return apiFetch<ProductView[]>('/catalog/products', { auth: false, query: filters });
}

export function publicSeller(sellerId: string) {
  return apiFetch<PublicSeller>(`/catalog/sellers/${sellerId}`, { auth: false });
}
