export type BackupStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface BackupJob {
  backupId: string;
  status: BackupStatus;
  source: string;
  destination: string;
  workers: number;
  filesDiscovered: number;
  filesProcessed: number;
  filesSkipped: number;
  filesDeduplicated: number;
  filesIncrementalSkipped: number;
  filesFailed: number;
  originalBytes: number;
  storedBytes: number;
  spaceSavedPercentage: number;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  durationMs?: number | null;
  errorMessage?: string | null;
}

export type BackupEventType =
  | 'BACKUP_STARTED'
  | 'FILE_DISCOVERED'
  | 'FILE_PROCESSED'
  | 'FILE_SKIPPED'
  | 'FILE_DEDUPLICATED'
  | 'FILE_FAILED'
  | 'BACKUP_COMPLETED'
  | 'BACKUP_FAILED';

export interface BackupEvent {
  backupId: string;
  type: BackupEventType;
  timestamp: string;
  file?: string | null;
  fileSize?: number | null;
  filesDiscovered: number;
  filesProcessed: number;
  filesSkipped: number;
  filesDeduplicated: number;
  filesIncrementalSkipped: number;
  filesFailed: number;
  storedBytes: number;
  spaceSavedPercentage: number;
  message?: string | null;
}

export interface BackupRequest {
  source: string;
  destination?: string;
  workers?: number;
}

export interface RestoreRequest {
  targetDirectory?: string;
}

export interface RestoreResponse {
  status: string;
  message: string;
  restoredFilesCount: number;
  timestamp: string;
}

export interface CatalogSummary {
  totalFiles: number;
  uniqueFiles: number;
  totalOriginalBytes: number;
  totalStoredBytes: number;
  deduplicatedBytes: number;
  spaceSavedPercentage: number;
  totalBackups: number;
  lastBackupTime?: string | null;
}

export interface CatalogFile {
  originalPath: string;
  hash: string;
  backupPath: string;
  originalSize: number;
  compressedSize: number;
  backupTime: string;
  lastModifiedTime: number;
  deleted: boolean;
  deduplicated: boolean;
}

export interface CatalogPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasMore: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

