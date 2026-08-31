import React from 'react';
import { LucideIcon, Inbox } from 'lucide-react';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export function EmptyState({
  icon: Icon = Inbox,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-12 text-center border border-dashed border-zinc-800 rounded-lg bg-zinc-900/20',
        className
      )}
    >
      <div className="p-3 bg-zinc-800/60 rounded-full text-zinc-400 mb-4">
        <Icon className="w-6 h-6" />
      </div>
      <h3 className="text-base font-semibold text-zinc-200">{title}</h3>
      <p className="text-sm text-zinc-400 mt-1 max-w-sm">{description}</p>
      {action && (
        <button
          onClick={action.onClick}
          className="mt-5 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-zinc-950 font-medium text-xs rounded-md transition-colors font-mono uppercase tracking-wider"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}

