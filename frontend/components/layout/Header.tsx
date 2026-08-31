'use client';

import React from 'react';
import { Plus, Server } from 'lucide-react';

interface HeaderProps {
  onOpenNewBackup: () => void;
}

export function Header({ onOpenNewBackup }: HeaderProps) {
  return (
    <header className="h-16 shrink-0 bg-zinc-950/80 backdrop-blur-xs border-b border-zinc-800/80 px-8 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 px-2.5 py-1 rounded-full border border-emerald-500/20 bg-emerald-500/10 text-emerald-400 text-xs font-mono">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
          <span>System Ready</span>
        </div>
        <div className="hidden md:flex items-center gap-2 text-xs font-mono text-zinc-500">
          <Server className="w-3.5 h-3.5" />
          <span>Spring Boot REST API:8080</span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button
          onClick={onOpenNewBackup}
          className="flex items-center gap-2 px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-zinc-950 font-mono font-semibold text-xs uppercase tracking-wider rounded-md transition-colors shadow-xs"
        >
          <Plus className="w-3.5 h-3.5 stroke-[3]" />
          <span>New Backup</span>
        </button>
      </div>
    </header>
  );
}

