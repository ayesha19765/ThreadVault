import type { Metadata } from 'next';
import './globals.css';
import { AppShell } from '@/components/layout/AppShell';

export const metadata: Metadata = {
  title: 'ThreadVault — Concurrent Backup & Deduplication Engine',
  description: 'High-performance concurrent backup and content-based deduplication dashboard',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full antialiased dark">
      <body className="min-h-full flex flex-col bg-zinc-950 text-zinc-100 font-sans">
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
