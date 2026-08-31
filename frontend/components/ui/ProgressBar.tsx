import React from 'react';
import { cn } from '@/lib/utils';

interface ProgressBarProps {
  current: number;
  total: number;
  showPercentage?: boolean;
  className?: string;
  barClassName?: string;
}

export function ProgressBar({
  current,
  total,
  showPercentage = true,
  className,
  barClassName,
}: ProgressBarProps) {
  const percentage = total > 0 ? Math.min(100, Math.max(0, (current / total) * 100)) : 0;
  const formattedPct = percentage.toFixed(1);

  return (
    <div className={cn('w-full', className)}>
      <div className="flex items-center justify-between text-xs font-mono text-zinc-400 mb-2">
        <span>
          {current.toLocaleString()} / {total.toLocaleString()} files
        </span>
        {showPercentage && <span className="font-semibold text-zinc-200">{formattedPct}%</span>}
      </div>
      <div className="w-full h-2.5 bg-zinc-800 rounded-full overflow-hidden border border-zinc-700/50">
        <div
          className={cn(
            'h-full bg-emerald-500 rounded-full transition-all duration-300 ease-out',
            barClassName
          )}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}

