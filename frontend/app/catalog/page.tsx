'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { getCatalogSummary, getCatalogFiles } from '@/lib/api/catalog';
import type { CatalogSummary, CatalogFile, CatalogPage as CatalogPageType } from '@/types';
import { StatCard } from '@/components/ui/StatCard';
import { Skeleton, TableSkeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatBytes, formatDate, truncateHash } from '@/lib/utils';
import {
  Database,
  Files,
  HardDrive,
  TrendingDown,
  Search,
  Hash,
  ChevronLeft,
  ChevronRight,
  Copy,
  Check,
  FilterX,
  FileCode,
} from 'lucide-react';

export default function CatalogPage() {
  const [summary, setSummary] = useState<CatalogSummary | null>(null);
  const [pageData, setPageData] = useState<CatalogPageType<CatalogFile> | null>(null);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [pathFilter, setPathFilter] = useState('');
  const [hashFilter, setHashFilter] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [copiedHash, setCopiedHash] = useState<string | null>(null);

  const fetchSummary = useCallback(async () => {
    try {
      const data = await getCatalogSummary();
      setSummary(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch catalog summary');
    } finally {
      setLoadingSummary(false);
    }
  }, []);

  const fetchFiles = useCallback(async () => {
    try {
      setError(null);
      const data = await getCatalogFiles({
        path: pathFilter,
        hash: hashFilter,
        page,
        size,
      });
      setPageData(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch catalog files');
    } finally {
      setLoadingFiles(false);
    }
  }, [pathFilter, hashFilter, page, size]);

  useEffect(() => {
    let active = true;
    getCatalogSummary()
      .then((data) => {
        if (active) {
          setSummary(data);
          setLoadingSummary(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Failed to fetch catalog summary');
          setLoadingSummary(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    getCatalogFiles({
      path: pathFilter,
      hash: hashFilter,
      page,
      size,
    })
      .then((data) => {
        if (active) {
          setPageData(data);
          setLoadingFiles(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Failed to fetch catalog files');
          setLoadingFiles(false);
        }
      });
    return () => {
      active = false;
    };
  }, [pathFilter, hashFilter, page, size]);

  const handleCopyHash = (hash: string) => {
    navigator.clipboard.writeText(hash);
    setCopiedHash(hash);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  const handleClearFilters = () => {
    setPathFilter('');
    setHashFilter('');
    setPage(0);
  };

  const handleRetry = () => {
    setLoadingSummary(true);
    setLoadingFiles(true);
    fetchSummary();
    fetchFiles();
  };

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div>
        <h1 className="text-xl font-mono font-bold tracking-tight text-zinc-100">
          Metadata Catalog & Storage Index
        </h1>
        <p className="text-xs font-mono text-zinc-400 mt-1">
          Explore content-addressed backup archives, deduplication references, and file metadata
        </p>
      </div>

      {error && <ErrorAlert message={error} onRetry={handleRetry} />}

      {/* Summary Cards */}
      {loadingSummary ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 rounded-lg" />
          ))}
        </div>
      ) : summary ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Total Cataloged Files"
            value={summary.totalFiles.toLocaleString()}
            subValue={`${summary.uniqueFiles.toLocaleString()} unique archives`}
            icon={Files}
          />
          <StatCard
            label="Original File Size"
            value={formatBytes(summary.totalOriginalBytes)}
            subValue="Uncompressed data volume"
            icon={Database}
          />
          <StatCard
            label="Stored Archive Size"
            value={formatBytes(summary.totalStoredBytes)}
            subValue="Physical disk occupancy"
            icon={HardDrive}
          />
          <StatCard
            label="Deduplication Savings"
            value={`${summary.spaceSavedPercentage.toFixed(1)}%`}
            subValue={`Saved ${formatBytes(summary.deduplicatedBytes)}`}
            icon={TrendingDown}
            className="border-emerald-500/30 bg-emerald-500/5"
          />
        </div>
      ) : null}

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-zinc-900/40 p-4 border border-zinc-800 rounded-lg">
        <div className="flex flex-1 flex-col sm:flex-row gap-3 w-full">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search by file path..."
              value={pathFilter}
              onChange={(e) => {
                setPathFilter(e.target.value);
                setPage(0);
              }}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-md pl-9 pr-3 py-1.5 text-xs font-mono text-zinc-200 placeholder:text-zinc-600 focus:outline-hidden focus:border-emerald-500/80"
            />
          </div>

          <div className="relative flex-1">
            <Hash className="w-4 h-4 text-zinc-500 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Filter by SHA-256 hash..."
              value={hashFilter}
              onChange={(e) => {
                setHashFilter(e.target.value);
                setPage(0);
              }}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-md pl-9 pr-3 py-1.5 text-xs font-mono text-zinc-200 placeholder:text-zinc-600 focus:outline-hidden focus:border-emerald-500/80"
            />
          </div>
        </div>

        {(pathFilter || hashFilter) && (
          <button
            onClick={handleClearFilters}
            className="flex items-center gap-1 text-xs font-mono text-zinc-400 hover:text-zinc-200 px-3 py-1.5 rounded border border-zinc-800 bg-zinc-900 hover:bg-zinc-800 transition-colors"
          >
            <FilterX className="w-3.5 h-3.5" />
            <span>Clear</span>
          </button>
        )}
      </div>

      {/* Files Table */}
      {loadingFiles ? (
        <TableSkeleton rows={8} cols={6} />
      ) : !pageData || pageData.content.length === 0 ? (
        <EmptyState
          icon={FileCode}
          title="No Cataloged Files"
          description={
            pathFilter || hashFilter
              ? 'No files matching the active search query were found in the catalog.'
              : 'The catalog is currently empty. Run a backup to index files.'
          }
          action={
            pathFilter || hashFilter
              ? { label: 'Clear Filters', onClick: handleClearFilters }
              : undefined
          }
        />
      ) : (
        <div className="space-y-4">
          <div className="border border-zinc-800 rounded-lg overflow-hidden bg-zinc-900/30">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-zinc-900/80 border-b border-zinc-800 text-zinc-400 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Original File Path</th>
                    <th className="px-4 py-3 text-right">Original Size</th>
                    <th className="px-4 py-3 text-right">Stored Size</th>
                    <th className="px-4 py-3">SHA-256 Content Hash</th>
                    <th className="px-4 py-3 text-center">Deduplicated</th>
                    <th className="px-4 py-3">Backup Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/60 text-zinc-300">
                  {pageData.content.map((file) => (
                    <tr
                      key={`${file.originalPath}-${file.hash}`}
                      className="hover:bg-zinc-800/40 transition-colors"
                    >
                      <td className="px-4 py-3 text-zinc-200 font-mono">
                        <span className="truncate block max-w-md" title={file.originalPath}>
                          {file.originalPath}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-300 whitespace-nowrap">
                        {formatBytes(file.originalSize)}
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-300 whitespace-nowrap">
                        {formatBytes(file.compressedSize)}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <code className="text-[11px] bg-zinc-950 px-1.5 py-0.5 rounded border border-zinc-800 text-zinc-400">
                            {truncateHash(file.hash, 16)}
                          </code>
                          <button
                            onClick={() => handleCopyHash(file.hash)}
                            className="text-zinc-500 hover:text-zinc-300 p-1 rounded transition-colors"
                            title="Copy full SHA-256 hash"
                          >
                            {copiedHash === file.hash ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center whitespace-nowrap">
                        {file.deduplicated ? (
                          <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium">
                            DEDUP
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-zinc-800/60 text-zinc-500 border border-zinc-700/40">
                            UNIQUE
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-zinc-400 whitespace-nowrap">
                        {formatDate(file.backupTime)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination Controls */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-xs font-mono text-zinc-400 px-1">
            <div>
              Showing {page * size + 1}–{Math.min((page + 1) * size, pageData.totalElements)} of{' '}
              {pageData.totalElements.toLocaleString()} files
            </div>

            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5">
                <span>Per page:</span>
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="bg-zinc-950 border border-zinc-800 rounded px-2 py-1 text-zinc-200"
                >
                  <option value={10}>10</option>
                  <option value={15}>15</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                </select>
              </div>

              <div className="flex items-center gap-1">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="p-1 rounded border border-zinc-800 bg-zinc-900 disabled:opacity-30 hover:bg-zinc-800 transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="px-2">
                  Page {page + 1} of {Math.max(1, pageData.totalPages)}
                </span>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={!pageData.hasMore}
                  className="p-1 rounded border border-zinc-800 bg-zinc-900 disabled:opacity-30 hover:bg-zinc-800 transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
