import React from 'react';
import { BackupStatus } from '@/types';
import { cn } from '@/lib/utils';
import { CheckCircle2, Clock, PlayCircle, XCircle, AlertCircle } from 'lucide-react';

interface StatusBadgeProps {
  status: BackupStatus | string;
  className?: string;
  showIcon?: boolean;
}

export function StatusBadge({ status, className, showIcon = true }: StatusBadgeProps) {
  const normalized = (status || '').toUpperCase() as BackupStatus;

  switch (normalized) {
    case 'COMPLETED':
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20',
            className
          )}
        >
          {showIcon && <CheckCircle2 className="w-3.5 h-3.5" />}
          COMPLETED
        </span>
      );
    case 'RUNNING':
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20 animate-pulse',
            className
          )}
        >
          {showIcon && <PlayCircle className="w-3.5 h-3.5 animate-spin" />}
          RUNNING
        </span>
      );
    case 'QUEUED':
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20',
            className
          )}
        >
          {showIcon && <Clock className="w-3.5 h-3.5" />}
          QUEUED
        </span>
      );
    case 'FAILED':
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20',
            className
          )}
        >
          {showIcon && <XCircle className="w-3.5 h-3.5" />}
          FAILED
        </span>
      );
    case 'CANCELLED':
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-zinc-500/10 text-zinc-600 dark:text-zinc-400 border border-zinc-500/20',
            className
          )}
        >
          {showIcon && <AlertCircle className="w-3.5 h-3.5" />}
          CANCELLED
        </span>
      );
    default:
      return (
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-zinc-500/10 text-zinc-600 border border-zinc-500/20',
            className
          )}
        >
          {status}
        </span>
      );
  }
}

