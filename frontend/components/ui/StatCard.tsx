import React from 'react';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: string | number;
  subValue?: string;
  icon?: LucideIcon;
  trend?: string;
  className?: string;
}

export function StatCard({ label, value, subValue, icon: Icon, className }: StatCardProps) {
  return (
    <div
      className={cn(
        'bg-zinc-900/50 border border-zinc-800 rounded-lg p-5 flex flex-col justify-between hover:border-zinc-700 transition-colors',
        className
      )}
    >
      <div className="flex items-center justify-between text-zinc-400 mb-3">
        <span className="text-xs font-mono tracking-wider uppercase">{label}</span>
        {Icon && <Icon className="w-4 h-4 text-zinc-500" />}
      </div>
      <div>
        <div className="text-2xl font-mono font-semibold tracking-tight text-zinc-100">
          {value}
        </div>
        {subValue && (
          <div className="text-xs font-mono text-zinc-400 mt-1 flex items-center gap-1.5">
            {subValue}
          </div>
        )}
      </div>
    </div>
  );
}

