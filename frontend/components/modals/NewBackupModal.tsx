'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { createBackup } from '@/lib/api/backups';
import { X, Play, Loader2, Cpu, Folder, HardDrive } from 'lucide-react';

interface NewBackupModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NewBackupModal({ isOpen, onClose }: NewBackupModalProps) {
  const router = useRouter();
  const [source, setSource] = useState('sample_data');
  const [destination, setDestination] = useState('backup_storage');
  const [workers, setWorkers] = useState<number>(4);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!source.trim()) {
      setError('Source directory path cannot be blank');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      const job = await createBackup({
        source: source.trim(),
        destination: destination.trim() || undefined,
        workers: workers > 0 ? workers : 4,
      });

      onClose();
      router.push(`/backups/${job.backupId}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to start backup job');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-800 bg-zinc-900/80">
          <div className="flex items-center gap-2.5">
            <div className="p-1.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Play className="w-4 h-4 fill-emerald-400" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-zinc-100">Initiate Backup</h2>
              <p className="text-xs text-zinc-400">Configure concurrent backup pipeline</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-200 p-1 rounded-md hover:bg-zinc-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3 text-xs bg-rose-500/10 border border-rose-500/20 text-rose-300 rounded-md">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-mono uppercase tracking-wider text-zinc-400 mb-1.5">
              Source Directory
            </label>
            <div className="relative">
              <Folder className="w-4 h-4 text-zinc-500 absolute left-3 top-2.5" />
              <input
                type="text"
                value={source}
                onChange={(e) => setSource(e.target.value)}
                placeholder="e.g. sample_data or /path/to/directory"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-md pl-9 pr-3 py-2 text-sm font-mono text-zinc-200 placeholder:text-zinc-600 focus:outline-hidden focus:border-emerald-500/80 focus:ring-1 focus:ring-emerald-500/50"
                required
              />
            </div>
            <p className="text-[11px] text-zinc-500 mt-1">Directory containing files to scan and back up</p>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase tracking-wider text-zinc-400 mb-1.5">
              Destination Directory (Optional)
            </label>
            <div className="relative">
              <HardDrive className="w-4 h-4 text-zinc-500 absolute left-3 top-2.5" />
              <input
                type="text"
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                placeholder="backup_storage"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-md pl-9 pr-3 py-2 text-sm font-mono text-zinc-200 placeholder:text-zinc-600 focus:outline-hidden focus:border-emerald-500/80 focus:ring-1 focus:ring-emerald-500/50"
              />
            </div>
            <p className="text-[11px] text-zinc-500 mt-1">Target folder for compressed content-addressed archives</p>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase tracking-wider text-zinc-400 mb-1.5">
              Worker Threads
            </label>
            <div className="flex items-center gap-2">
              {[2, 4, 8, 16].map((count) => (
                <button
                  type="button"
                  key={count}
                  onClick={() => setWorkers(count)}
                  className={`flex-1 py-1.5 text-xs font-mono rounded border transition-colors ${
                    workers === count
                      ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-300 font-semibold'
                      : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:border-zinc-700'
                  }`}
                >
                  {count} Threads
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2 mt-2">
              <Cpu className="w-3.5 h-3.5 text-zinc-500" />
              <input
                type="number"
                min={1}
                max={64}
                value={workers}
                onChange={(e) => setWorkers(parseInt(e.target.value) || 4)}
                className="w-20 bg-zinc-950 border border-zinc-800 rounded px-2 py-1 text-xs font-mono text-zinc-200"
              />
              <span className="text-xs text-zinc-500 font-mono">Custom pool size</span>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-zinc-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-mono text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded-md transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-zinc-950 font-semibold text-xs font-mono uppercase tracking-wider rounded-md transition-colors"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  Starting...
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  Start Backup
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

