import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ErrorAlertProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorAlert({
  title = 'Failed to load data',
  message,
  onRetry,
  className,
}: ErrorAlertProps) {
  return (
    <div
      className={cn(
        'flex items-start gap-3 p-4 rounded-lg border border-rose-500/20 bg-rose-500/10 text-rose-300',
        className
      )}
    >
      <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
      <div className="flex-1 text-sm">
        <div className="font-semibold text-rose-200">{title}</div>
        <div className="text-rose-300/90 mt-0.5">{message}</div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-mono font-medium rounded border border-rose-500/30 hover:bg-rose-500/20 text-rose-200 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Retry
        </button>
      )}
    </div>
  );
}

