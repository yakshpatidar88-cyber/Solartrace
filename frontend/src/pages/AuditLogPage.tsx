import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { AuditEvent } from '../types';
import { History, ShieldCheck, User, Terminal } from 'lucide-react';

export const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getRecentAuditLogs().then((data) => {
      setLogs(data);
      setLoading(false);
    });
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">System Audit & Verification Trail</h1>
        <p className="text-sm text-slate-400 mt-1">
          Immutable sequence of anomaly triggers, priority calculations, technician dispatches, and verification results.
        </p>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <Terminal className="h-4 w-4 text-emerald-400" />
            <span className="font-mono">LEDGER: Operational Event Log</span>
          </div>
          <span>Showing latest {logs.length} events</span>
        </div>

        <div className="divide-y divide-slate-800/60">
          {logs.map((log) => (
            <div key={log.id} className="p-4 hover:bg-slate-800/40 transition-colors flex items-start gap-4">
              <div className="p-2 rounded-lg bg-slate-800 text-slate-400 mt-0.5 flex-shrink-0">
                <History className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      {log.action}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">[{log.entityType}]</span>
                  </div>
                  <span className="text-xs text-slate-500">
                    {new Date(log.createdAt).toLocaleString()}
                  </span>
                </div>
                <p className="text-sm text-slate-200 mt-2 font-mono break-words">{log.details}</p>
                <div className="mt-1 flex items-center gap-2 text-[11px] text-slate-500">
                  <User className="h-3 w-3" />
                  <span>Agent / Actor: <strong className="text-slate-400">{log.performedBy}</strong></span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
