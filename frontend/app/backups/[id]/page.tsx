'use client';

import React from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useBackupStream } from '@/lib/sse/useBackupStream';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { StatCard } from '@/components/ui/StatCard';
import { Skeleton } from '@/components/ui/Skeleton';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatBytes, formatDuration, formatDate } from '@/lib/utils';
import {
  ArrowLeft,
  ArrowRight,
  Database,
  HardDrive,
  Cpu,
  CheckCircle2,
  XCircle,
  Activity,
  Layers,
  FileCheck2,
  FileMinus,
  CopyCheck,
  Radio,
} from 'lucide-react';

export default function BackupDetailsPage() {
  const params = useParams();
  const backupId = params?.id as string;

  const { job, events, isConnected, isCompleted, isFailed, error, refetch } =
    useBackupStream(backupId);

  if (error && !job) {
    return (
      <div className="space-y-6">
        <Link
          href="/backups"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-zinc-400 hover:text-zinc-200 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Backups</span>
        </Link>
        <ErrorAlert message={error} onRetry={refetch} />
      </div>
    );
  }

  if (!job) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-40 w-full rounded-xl" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Breadcrumbs */}
      <div className="flex items-center justify-between">
        <Link
          href="/backups"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-zinc-400 hover:text-zinc-200 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Backups</span>
        </Link>

        {/* Live SSE status indicator */}
        <div className="flex items-center gap-2 text-xs font-mono">
          {isConnected ? (
            <span className="flex items-center gap-1.5 text-emerald-400">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
              <span>Live Stream Connected</span>
            </span>
          ) : isCompleted ? (
            <span className="text-zinc-400">Stream Closed (Job Completed)</span>
          ) : isFailed ? (
            <span className="text-rose-400">Stream Closed (Job Failed)</span>
          ) : (
            <span className="text-zinc-500">Connecting to Stream...</span>
          )}
        </div>
      </div>

      {/* Main Execution Card */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-xl p-6 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/80 pb-5">
          <div>
            <div className="flex items-center gap-2.5">
              <StatusBadge status={job.status} className="text-xs px-3 py-1" />
              <span className="text-xs font-mono text-zinc-500">ID: {job.backupId}</span>
            </div>
            <div className="flex items-center gap-2 mt-2 text-base font-mono font-semibold text-zinc-100">
              <span>{job.source}</span>
              <ArrowRight className="w-4 h-4 text-zinc-500" />
              <span className="text-emerald-400">{job.destination}</span>
            </div>
          </div>

          <div className="flex items-center gap-6 text-xs font-mono text-zinc-400">
            <div className="flex items-center gap-1.5">
              <Cpu className="w-4 h-4 text-zinc-500" />
              <span>{job.workers} Worker Threads</span>
            </div>
            <div>
              <span className="text-zinc-500">Created:</span> {formatDate(job.createdAt)}
            </div>
          </div>
        </div>

        {/* Status Banners */}
        {isCompleted ? (
          <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-lg p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <CheckCircle2 className="w-6 h-6 text-emerald-400 shrink-0" />
              <div>
                <div className="text-sm font-semibold text-emerald-300 font-mono">
                  BACKUP COMPLETED SUCCESSFULLY
                </div>
                <div className="text-xs text-emerald-400/80 font-mono mt-0.5">
                  Processed {job.filesProcessed.toLocaleString()} files • Saved {job.spaceSavedPercentage.toFixed(1)}% disk space in {formatDuration(job.durationMs)}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <Link
                href="/catalog"
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-zinc-950 font-mono font-semibold text-xs rounded transition-colors"
              >
                View Catalog
              </Link>
            </div>
          </div>
        ) : isFailed ? (
          <div className="bg-rose-500/10 border border-rose-500/20 rounded-lg p-4 flex items-center gap-3">
            <XCircle className="w-6 h-6 text-rose-400 shrink-0" />
            <div>
              <div className="text-sm font-semibold text-rose-300 font-mono">
                BACKUP JOB FAILED
              </div>
              <div className="text-xs text-rose-300/80 font-mono mt-0.5">
                {job.errorMessage || 'An error occurred during concurrent backup execution.'}
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-zinc-300 font-semibold uppercase tracking-wider flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-blue-500 animate-ping"></span>
                Backup In Progress
              </span>
              <span className="text-zinc-400">Duration: {formatDuration(job.durationMs)}</span>
            </div>
            <ProgressBar
              current={job.filesProcessed + job.filesSkipped}
              total={job.filesDiscovered || Math.max(1, job.filesProcessed + job.filesSkipped)}
            />
          </div>
        )}

        {/* Live Counters Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <StatCard
            label="Discovered"
            value={job.filesDiscovered.toLocaleString()}
            icon={Layers}
            className="p-3"
          />
          <StatCard
            label="Processed"
            value={job.filesProcessed.toLocaleString()}
            icon={FileCheck2}
            className="p-3"
          />
          <StatCard
            label="Skipped"
            value={job.filesIncrementalSkipped.toLocaleString()}
            subValue="Unchanged"
            icon={FileMinus}
            className="p-3"
          />
          <StatCard
            label="Deduplicated"
            value={job.filesDeduplicated.toLocaleString()}
            subValue="Duplicate content"
            icon={CopyCheck}
            className="p-3"
          />
          <StatCard
            label="Stored Data"
            value={formatBytes(job.storedBytes)}
            icon={HardDrive}
            className="p-3"
          />
          <StatCard
            label="Space Saved"
            value={`${job.spaceSavedPercentage.toFixed(1)}%`}
            icon={Database}
            className="p-3 border-emerald-500/20 bg-emerald-500/5"
          />
        </div>
      </div>

      {/* Live Activity Stream Feed */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-mono text-zinc-400 uppercase tracking-wider">
            <Activity className="w-4 h-4 text-emerald-400" />
            <span>Live Activity Log</span>
            <span className="text-zinc-600">({events.length} events buffered)</span>
          </div>
        </div>

        <div className="border border-zinc-800 rounded-lg bg-zinc-950/60 overflow-hidden">
          {events.length === 0 ? (
            <div className="p-8 text-center text-xs font-mono text-zinc-500">
              {isCompleted
                ? 'Job completed before live logging session started.'
                : 'Waiting for backup worker events...'}
            </div>
          ) : (
            <div className="divide-y divide-zinc-900 max-h-96 overflow-y-auto">
              {events.map((evt, idx) => (
                <div
                  key={`${evt.timestamp}-${idx}`}
                  className="px-4 py-2.5 flex items-center justify-between text-xs font-mono hover:bg-zinc-900/40 transition-colors"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="text-[10px] text-zinc-500 whitespace-nowrap">
                      {formatDate(evt.timestamp)}
                    </span>

                    {evt.type === 'FILE_PROCESSED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shrink-0 font-medium">
                        PROCESSED
                      </span>
                    )}
                    {evt.type === 'FILE_DEDUPLICATED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-blue-500/10 text-blue-400 border border-blue-500/20 shrink-0 font-medium">
                        DEDUPLICATED
                      </span>
                    )}
                    {evt.type === 'FILE_SKIPPED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-zinc-500/10 text-zinc-400 border border-zinc-500/20 shrink-0 font-medium">
                        UNCHANGED
                      </span>
                    )}
                    {evt.type === 'FILE_DISCOVERED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-purple-500/10 text-purple-400 border border-purple-500/20 shrink-0 font-medium">
                        DISCOVERED
                      </span>
                    )}
                    {evt.type === 'BACKUP_STARTED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-amber-500/10 text-amber-400 border border-amber-500/20 shrink-0 font-medium">
                        STARTED
                      </span>
                    )}
                    {evt.type === 'BACKUP_COMPLETED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shrink-0 font-medium">
                        COMPLETED
                      </span>
                    )}
                    {evt.type === 'BACKUP_FAILED' && (
                      <span className="px-2 py-0.5 rounded text-[10px] bg-rose-500/10 text-rose-400 border border-rose-500/20 shrink-0 font-medium">
                        FAILED
                      </span>
                    )}

                    <span className="text-zinc-300 truncate">
                      {evt.file || evt.message || 'Operation executed'}
                    </span>
                  </div>

                  {evt.fileSize !== undefined && evt.fileSize !== null && (
                    <div className="text-[11px] text-zinc-500 shrink-0 ml-4 font-mono">
                      {formatBytes(evt.fileSize)}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

