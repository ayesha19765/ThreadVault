import { API_BASE_URL, fetchJson } from './config';
import { BackupJob, BackupRequest, RestoreRequest, RestoreResponse } from '@/types';

export async function createBackup(req: BackupRequest): Promise<BackupJob> {
  return fetchJson<BackupJob>(`${API_BASE_URL}/api/backups`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function getBackup(id: string): Promise<BackupJob> {
  return fetchJson<BackupJob>(`${API_BASE_URL}/api/backups/${id}`);
}

export async function listBackups(): Promise<BackupJob[]> {
  return fetchJson<BackupJob[]>(`${API_BASE_URL}/api/backups`);
}

export async function restoreBackup(id: string, req: RestoreRequest = {}): Promise<RestoreResponse> {
  return fetchJson<RestoreResponse>(`${API_BASE_URL}/api/backups/${id}/restore`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

