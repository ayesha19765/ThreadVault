'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, History, Database, RotateCcw, ShieldCheck } from 'lucide-react';
import { cn } from '@/lib/utils';

const NAV_ITEMS = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Backups', href: '/backups', icon: History },
  { label: 'Catalog', href: '/catalog', icon: Database },
  { label: 'Restore', href: '/restore', icon: RotateCcw },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 shrink-0 bg-zinc-950 border-r border-zinc-800/80 flex flex-col justify-between h-screen sticky top-0">
      <div>
        {/* Brand Header */}
        <div className="h-16 flex items-center gap-3 px-6 border-b border-zinc-800/80">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="font-mono font-bold text-sm text-zinc-100 tracking-tight">ThreadVault</div>
            <div className="text-[10px] font-mono text-zinc-500 uppercase tracking-widest">Backup Engine</div>
          </div>
        </div>

        {/* Navigation List */}
        <nav className="p-4 space-y-1">
          <div className="px-3 py-2 text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-semibold">
            Platform
          </div>
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href));
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-mono transition-all',
                  isActive
                    ? 'bg-zinc-800/80 text-emerald-400 font-semibold border border-zinc-700/60'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/60'
                )}
              >
                <Icon className={cn('w-4 h-4', isActive ? 'text-emerald-400' : 'text-zinc-500')} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Engine Status / Footer */}
      <div className="p-4 m-3 rounded-lg border border-zinc-800/60 bg-zinc-900/40">
        <div className="flex items-center gap-2 text-xs font-mono text-zinc-400">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
          <span className="text-zinc-300 font-medium">Core Engine</span>
        </div>
        <div className="text-[11px] font-mono text-zinc-500 mt-1">
          Java 21 • Concurrent NIO
        </div>
      </div>
    </aside>
  );
}

