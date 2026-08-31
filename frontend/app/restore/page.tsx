'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { listBackups, restoreBackup } from '@/lib/api/backups';
import { BackupJob, RestoreResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatBytes, formatDate } from '@/lib/utils';
import { RotateCcw, CheckCircle2, Loader2, Folder, HardDrive } from 'lucide-react';

export default function RestorePage() {
  const [backups, setBackups] = useState<BackupJob[]>([]);
  const [selectedBackupId, setSelectedBackupId] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [isRestoring, setIsRestoring] = useState(false);
  const [restoreResult, setRestoreResult] = useState<RestoreResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchBackups = useCallback(async () => {
    try {
      const data = await listBackups();
      setBackups(data);
      if (data.length > 0) {
        setSelectedBackupId((prev) => prev || data[0].backupId);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load backups');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    listBackups()
      .then((data) => {
        if (active) {
          setBackups(data);
          if (data.length > 0) {
            setSelectedBackupId((prev) => prev || data[0].backupId);
          }
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Failed to load backups');
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const handleRetry = () => {
    setLoading(true);
    fetchBackups();
  };

  const handleRestore = async () => {
    if (!selectedBackupId) return;

    try {
      setIsRestoring(true);
      setError(null);
      setRestoreResult(null);

      const res = await restoreBackup(selectedBackupId);
      setRestoreResult(res);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Restore operation failed');
    } finally {
      setIsRestoring(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      {/* Page Header */}
      <div>
        <h1 className="text-xl font-mono font-bold tracking-tight text-zinc-100">
          Restore Engine
        </h1>
        <p className="text-xs font-mono text-zinc-400 mt-1">
          Extract backed-up content-addressed ZIP archives back into the restore target directory
        </p>
      </div>

      {error && <ErrorAlert message={error} onRetry={handleRetry} />}

      {/* Restore Notification Result */}
      {restoreResult && (
        <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-lg p-5 flex items-start gap-3 animate-in fade-in duration-200">
          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
          <div className="flex-1 space-y-1">
            <div className="text-sm font-semibold text-emerald-300 font-mono">
              {restoreResult.message || 'Restoration Completed'}
            </div>
            <div className="text-xs font-mono text-emerald-400/80">
              Successfully restored {restoreResult.restoredFilesCount} files to destination directory.
            </div>
            <div className="text-[11px] font-mono text-zinc-500 pt-1">
              Restored at {formatDate(restoreResult.timestamp)}
            </div>
          </div>
        </div>
      )}

      {/* Restore Controller Form */}
      <div className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-6 space-y-6">
        <div className="flex items-center gap-2.5 border-b border-zinc-800 pb-4">
          <div className="p-1.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <RotateCcw className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-zinc-100 font-mono">
              Execute Repository Restoration
            </h2>
            <p className="text-xs text-zinc-500 font-mono">
              Select a catalog snapshot to restore original files
            </p>
          </div>
        </div>

        {loading ? (
          <div className="space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-32" />
          </div>
        ) : backups.length === 0 ? (
          <EmptyState
            icon={RotateCcw}
            title="No Backup Snapshots Available"
            description="You need at least one completed backup operation before you can perform a restore."
          />
        ) : (
          <div className="space-y-5">
            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-zinc-400 mb-2">
                Target Backup Snapshot
              </label>
              <div className="space-y-2">
                {backups.map((job) => (
                  <label
                    key={job.backupId}
                    className={`flex items-center justify-between p-3.5 rounded-lg border text-xs font-mono cursor-pointer transition-colors ${
                      selectedBackupId === job.backupId
                        ? 'border-emerald-500/60 bg-emerald-500/5 text-zinc-100'
                        : 'border-zinc-800 bg-zinc-950/40 text-zinc-400 hover:border-zinc-700'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="backupSelection"
                        value={job.backupId}
                        checked={selectedBackupId === job.backupId}
                        onChange={() => setSelectedBackupId(job.backupId)}
                        className="accent-emerald-500"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <Folder className="w-3.5 h-3.5 text-zinc-500" />
                          <span className="font-semibold text-zinc-200">{job.source}</span>
                          <StatusBadge status={job.status} className="text-[10px] py-0 px-2" />
                        </div>
                        <div className="text-[11px] text-zinc-500 mt-0.5">
                          ID: {job.backupId} • Created: {formatDate(job.createdAt)}
                        </div>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <div className="text-zinc-200">{formatBytes(job.storedBytes)}</div>
                      <div className="text-[11px] text-zinc-500">
                        {job.filesProcessed} files
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div className="p-4 rounded-lg bg-zinc-950 border border-zinc-800/80 text-xs font-mono text-zinc-400 space-y-1">
              <div className="flex items-center gap-2 text-zinc-300 font-semibold">
                <HardDrive className="w-4 h-4 text-emerald-400" />
                <span>Restoration Target</span>
              </div>
              <p className="text-[11px] text-zinc-500">
                Extracted files will be safely reconstructed inside the designated <code>restore/</code> directory preserving original file hierarchy.
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={handleRestore}
                disabled={isRestoring || !selectedBackupId}
                className="flex items-center gap-2 px-5 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-zinc-950 font-semibold text-xs font-mono uppercase tracking-wider rounded-md transition-colors shadow-xs"
              >
                {isRestoring ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Restoring Files...
                  </>
                ) : (
                  <>
                    <RotateCcw className="w-4 h-4" />
                    Restore Files Now
                  </>
                )}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

