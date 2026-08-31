'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { BackupEvent, BackupJob, BackupStatus } from '@/types';
import { API_BASE_URL } from '@/lib/api/config';
import { getBackup } from '@/lib/api/backups';

export interface UseBackupStreamResult {
  job: BackupJob | null;
  events: BackupEvent[];
  isConnected: boolean;
  isCompleted: boolean;
  isFailed: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useBackupStream(backupId: string): UseBackupStreamResult {
  const [job, setJob] = useState<BackupJob | null>(null);
  const [events, setEvents] = useState<BackupEvent[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const isCompleted = job?.status === 'COMPLETED';
  const isFailed = job?.status === 'FAILED';

  const fetchInitialState = useCallback(async () => {
    try {
      setError(null);
      const data = await getBackup(backupId);
      setJob(data);
      return data;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch backup details';
      setError(msg);
      return null;
    }
  }, [backupId]);

  useEffect(() => {
    let isMounted = true;

    async function init() {
      const initialJob = await fetchInitialState();
      if (!isMounted || !initialJob) return;

      // If job already finished, no need to keep stream open
      if (initialJob.status === 'COMPLETED' || initialJob.status === 'FAILED') {
        setIsConnected(false);
        return;
      }

      // Close any existing connection
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      const streamUrl = `${API_BASE_URL}/api/backups/${backupId}/stream`;
      const es = new EventSource(streamUrl);
      eventSourceRef.current = es;

      es.onopen = () => {
        if (isMounted) setIsConnected(true);
      };

      const handleEventData = (type: string, rawData: string) => {
        try {
          const parsed = JSON.parse(rawData);

          if (type === 'INITIAL_STATE') {
            const initialSnapshot = parsed as BackupJob;
            setJob(initialSnapshot);
            return;
          }

          const evt = parsed as BackupEvent;
          setEvents((prev) => [evt, ...prev.slice(0, 99)]); // Keep last 100 events

          // Update job state incrementally from event
          setJob((prevJob) => {
            if (!prevJob) return prevJob;

            let nextStatus: BackupStatus = prevJob.status;
            if (evt.type === 'BACKUP_STARTED') nextStatus = 'RUNNING';
            else if (evt.type === 'BACKUP_COMPLETED') nextStatus = 'COMPLETED';
            else if (evt.type === 'BACKUP_FAILED') nextStatus = 'FAILED';
            else if (prevJob.status === 'QUEUED') nextStatus = 'RUNNING';

            return {
              ...prevJob,
              status: nextStatus,
              filesDiscovered: evt.filesDiscovered || prevJob.filesDiscovered,
              filesProcessed: evt.filesProcessed !== undefined ? evt.filesProcessed : prevJob.filesProcessed,
              filesSkipped: evt.filesSkipped !== undefined ? evt.filesSkipped : prevJob.filesSkipped,
              filesDeduplicated: evt.filesDeduplicated !== undefined ? evt.filesDeduplicated : prevJob.filesDeduplicated,
              filesIncrementalSkipped: evt.filesIncrementalSkipped !== undefined ? evt.filesIncrementalSkipped : prevJob.filesIncrementalSkipped,
              filesFailed: evt.filesFailed !== undefined ? evt.filesFailed : prevJob.filesFailed,
              storedBytes: evt.storedBytes || prevJob.storedBytes,
              spaceSavedPercentage: evt.spaceSavedPercentage || prevJob.spaceSavedPercentage,
              errorMessage: evt.type === 'BACKUP_FAILED' ? evt.message || 'Backup failed' : prevJob.errorMessage,
            };
          });

          if (evt.type === 'BACKUP_COMPLETED' || evt.type === 'BACKUP_FAILED') {
            es.close();
            if (isMounted) setIsConnected(false);
            // Refresh to get exact final completedAt and duration
            fetchInitialState();
          }
        } catch {
          // ignore parse errors
        }
      };

      const eventNames = [
        'INITIAL_STATE',
        'BACKUP_STARTED',
        'FILE_DISCOVERED',
        'FILE_PROCESSED',
        'FILE_SKIPPED',
        'FILE_DEDUPLICATED',
        'FILE_FAILED',
        'BACKUP_COMPLETED',
        'BACKUP_FAILED',
      ];

      eventNames.forEach((name) => {
        es.addEventListener(name, (e: MessageEvent) => {
          if (isMounted) handleEventData(name, e.data);
        });
      });

      es.onmessage = (e: MessageEvent) => {
        if (isMounted) handleEventData('GENERIC', e.data);
      };

      es.onerror = () => {
        if (isMounted) {
          setIsConnected(false);
          fetchInitialState();
        }
        es.close();
      };
    }

    init();

    return () => {
      isMounted = false;
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [backupId, fetchInitialState]);

  return {
    job,
    events,
    isConnected,
    isCompleted,
    isFailed,
    error,
    refetch: async () => {
      await fetchInitialState();
    },
  };
}
