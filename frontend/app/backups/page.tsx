'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { listBackups } from '@/lib/api/backups';
import { BackupJob } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Skeleton } from '@/components/ui/Skeleton';
import { formatBytes, formatDuration, formatDate } from '@/lib/utils';
import { History, RefreshCw, ArrowRight, Folder } from 'lucide-react';

export default function BackupsPage() {
  const [backups, setBackups] = useState<BackupJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBackups = useCallback(async () => {
    try {
      const data = await listBackups();
      setBackups(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch backup history');
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
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Failed to fetch backup history');
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const handleRefresh = () => {
    setLoading(true);
    fetchBackups();
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-mono font-bold tracking-tight text-zinc-100">
            Backup History
          </h1>
          <p className="text-xs font-mono text-zinc-400 mt-1">
            Historical execution logs and job performance metrics
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={loading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-mono rounded border border-zinc-800 bg-zinc-900/60 hover:bg-zinc-800 text-zinc-300 transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {error && <ErrorAlert message={error} onRetry={handleRefresh} />}

      {loading ? (
        <div className="space-y-3">
          <Skeleton className="h-10 w-full" />
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : backups.length === 0 ? (
        <EmptyState
          icon={History}
          title="No Backups Found"
          description="There are no recent backup jobs recorded in the application registry."
        />
      ) : (
        <div className="border border-zinc-800 rounded-lg overflow-hidden bg-zinc-900/30">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-zinc-900/80 border-b border-zinc-800 text-zinc-400 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Source</th>
                  <th className="px-4 py-3 text-center">Workers</th>
                  <th className="px-4 py-3 text-right">Processed</th>
                  <th className="px-4 py-3 text-right">Skipped</th>
                  <th className="px-4 py-3 text-right">Dedup</th>
                  <th className="px-4 py-3 text-right">Stored</th>
                  <th className="px-4 py-3 text-right">Saved</th>
                  <th className="px-4 py-3 text-right">Duration</th>
                  <th className="px-4 py-3 text-center">Inspect</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 text-zinc-300">
                {backups.map((job) => (
                  <tr
                    key={job.backupId}
                    className="hover:bg-zinc-800/40 transition-colors"
                  >
                    <td className="px-4 py-3 text-zinc-400 whitespace-nowrap">
                      {formatDate(job.createdAt)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <StatusBadge status={job.status} />
                    </td>
                    <td className="px-4 py-3 font-mono text-zinc-200">
                      <div className="flex items-center gap-1.5">
                        <Folder className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
                        <span className="truncate max-w-xs">{job.source}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-center text-zinc-400">
                      {job.workers}
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-200">
                      {job.filesProcessed.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-400">
                      {job.filesSkipped.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-400">
                      {job.filesDeduplicated.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-200 whitespace-nowrap">
                      {formatBytes(job.storedBytes)}
                    </td>
                    <td className="px-4 py-3 text-right text-emerald-400 whitespace-nowrap">
                      {job.spaceSavedPercentage.toFixed(1)}%
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-400 whitespace-nowrap">
                      {formatDuration(job.durationMs)}
                    </td>
                    <td className="px-4 py-3 text-center whitespace-nowrap">
                      <Link
                        href={`/backups/${job.backupId}`}
                        className="inline-flex items-center gap-1 text-emerald-400 hover:text-emerald-300 text-xs py-1 px-2 rounded hover:bg-emerald-500/10 transition-colors"
                      >
                        <span>Details</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

