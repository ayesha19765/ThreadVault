'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { getCatalogSummary } from '@/lib/api/catalog';
import { listBackups } from '@/lib/api/backups';
import { CatalogSummary, BackupJob } from '@/types';
import { StatCard } from '@/components/ui/StatCard';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatBytes, formatDuration, formatDate } from '@/lib/utils';
import {
  Files,
  HardDrive,
  Database,
  ArrowRight,
  TrendingDown,
  History,
  Layers,
} from 'lucide-react';

export default function DashboardPage() {
  const [catalog, setCatalog] = useState<CatalogSummary | null>(null);
  const [recentBackups, setRecentBackups] = useState<BackupJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    try {
      const [summaryData, backupsData] = await Promise.all([
        getCatalogSummary(),
        listBackups(),
      ]);
      setCatalog(summaryData);
      setRecentBackups(backupsData.slice(0, 5));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    Promise.all([getCatalogSummary(), listBackups()])
      .then(([summaryData, backupsData]) => {
        if (active) {
          setCatalog(summaryData);
          setRecentBackups(backupsData.slice(0, 5));
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const handleRetry = () => {
    setLoading(true);
    fetchDashboardData();
  };

  return (
    <div className="space-y-8">
      {/* Page Title */}
      <div>
        <h1 className="text-xl font-mono font-bold tracking-tight text-zinc-100">
          ThreadVault Dashboard
        </h1>
        <p className="text-xs font-mono text-zinc-400 mt-1">
          Concurrent Backup & Content-Based Deduplication Overview
        </p>
      </div>

      {error && <ErrorAlert message={error} onRetry={handleRetry} />}

      {/* Storage Metrics Grid */}
      <div>
        <div className="flex items-center gap-2 mb-3 text-xs font-mono text-zinc-400 uppercase tracking-wider">
          <Layers className="w-3.5 h-3.5 text-zinc-500" />
          <span>Storage & Deduplication Efficiency</span>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-28 rounded-lg" />
            ))}
          </div>
        ) : catalog ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard
              label="Total Files"
              value={catalog.totalFiles.toLocaleString()}
              subValue={`${catalog.uniqueFiles.toLocaleString()} unique content hashes`}
              icon={Files}
            />
            <StatCard
              label="Original Data"
              value={formatBytes(catalog.totalOriginalBytes)}
              subValue="Uncompressed source files"
              icon={Database}
            />
            <StatCard
              label="Stored Data"
              value={formatBytes(catalog.totalStoredBytes)}
              subValue="Compressed archives on disk"
              icon={HardDrive}
            />
            <StatCard
              label="Space Saved"
              value={`${catalog.spaceSavedPercentage.toFixed(1)}%`}
              subValue={`Saved ${formatBytes(catalog.deduplicatedBytes)} via dedup & zip`}
              icon={TrendingDown}
              className="border-emerald-500/30 bg-emerald-500/5"
            />
          </div>
        ) : null}
      </div>

      {/* Storage Ratio Bar (Visual Efficiency Breakdown) */}
      {catalog && catalog.totalOriginalBytes > 0 && (
        <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-5">
          <div className="flex items-center justify-between text-xs font-mono text-zinc-400 mb-3">
            <span className="font-semibold uppercase tracking-wider text-zinc-300">
              Storage Footprint Ratio
            </span>
            <span>
              {formatBytes(catalog.totalStoredBytes)} of {formatBytes(catalog.totalOriginalBytes)}
            </span>
          </div>

          <div className="h-3 w-full bg-zinc-800 rounded-full overflow-hidden flex border border-zinc-700/40">
            <div
              className="h-full bg-emerald-500 transition-all duration-500"
              style={{
                width: `${Math.min(
                  100,
                  (catalog.totalStoredBytes / catalog.totalOriginalBytes) * 100
                )}%`,
              }}
              title="Stored Data"
            />
          </div>

          <div className="flex items-center justify-between text-[11px] font-mono text-zinc-500 mt-2">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              <span>Stored Payload ({catalog.spaceSavedPercentage.toFixed(1)}% Reduction)</span>
            </div>
            <span>Catalog Total: {catalog.totalBackups} Backups</span>
          </div>
        </div>
      )}

      {/* Recent Backups Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-mono text-zinc-400 uppercase tracking-wider">
            <History className="w-3.5 h-3.5 text-zinc-500" />
            <span>Recent Backup Operations</span>
          </div>
          <Link
            href="/backups"
            className="text-xs font-mono text-emerald-400 hover:text-emerald-300 flex items-center gap-1 transition-colors"
          >
            <span>View all backups</span>
            <ArrowRight className="w-3 h-3" />
          </Link>
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-14 rounded-lg" />
            ))}
          </div>
        ) : recentBackups.length === 0 ? (
          <EmptyState
            title="No Backups Recorded"
            description="No backup jobs have been run yet. Initiate your first backup to populate the engine."
          />
        ) : (
          <div className="border border-zinc-800 rounded-lg overflow-hidden bg-zinc-900/30">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-zinc-900/80 border-b border-zinc-800 text-zinc-400 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Created</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Source</th>
                    <th className="px-4 py-3 text-right">Files</th>
                    <th className="px-4 py-3 text-right">Stored</th>
                    <th className="px-4 py-3 text-right">Saved</th>
                    <th className="px-4 py-3 text-right">Duration</th>
                    <th className="px-4 py-3 text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/60 text-zinc-300">
                  {recentBackups.map((job) => (
                    <tr
                      key={job.backupId}
                      className="hover:bg-zinc-800/40 transition-colors group cursor-pointer"
                    >
                      <td className="px-4 py-3 text-zinc-400">
                        {formatDate(job.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={job.status} />
                      </td>
                      <td className="px-4 py-3 font-mono text-zinc-200 truncate max-w-xs">
                        {job.source}
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-200">
                        {job.filesProcessed.toLocaleString()}
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-200">
                        {formatBytes(job.storedBytes)}
                      </td>
                      <td className="px-4 py-3 text-right text-emerald-400">
                        {job.spaceSavedPercentage.toFixed(1)}%
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-400">
                        {formatDuration(job.durationMs)}
                      </td>
                      <td className="px-4 py-3 text-center">
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
    </div>
  );
}

