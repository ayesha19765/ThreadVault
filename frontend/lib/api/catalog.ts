import { API_BASE_URL, fetchJson } from './config';
import { CatalogFile, CatalogPage, CatalogSummary } from '@/types';

export async function getCatalogSummary(): Promise<CatalogSummary> {
  return fetchJson<CatalogSummary>(`${API_BASE_URL}/api/catalog`);
}

export interface CatalogQueryOptions {
  path?: string;
  hash?: string;
  page?: number;
  size?: number;
}

export async function getCatalogFiles(
  options: CatalogQueryOptions = {}
): Promise<CatalogPage<CatalogFile>> {
  const params = new URLSearchParams();
  if (options.path && options.path.trim()) {
    params.set('path', options.path.trim());
  }
  if (options.hash && options.hash.trim()) {
    params.set('hash', options.hash.trim());
  }
  if (options.page !== undefined) {
    params.set('page', String(options.page));
  }
  if (options.size !== undefined) {
    params.set('size', String(options.size));
  }

  const query = params.toString();
  const url = `${API_BASE_URL}/api/catalog/files${query ? `?${query}` : ''}`;
  return fetchJson<CatalogPage<CatalogFile>>(url);
}

