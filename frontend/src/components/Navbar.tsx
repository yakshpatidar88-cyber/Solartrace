import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Zap, AlertTriangle, History, Activity, Radio } from 'lucide-react';
import { useWebSocket } from '../hooks/useWebSocket';

export const Navbar: React.FC = () => {
  const { isConnected } = useWebSocket();
  const navItems = [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/assets', label: 'Fleet & Assets', icon: Zap },
    { to: '/incidents', label: 'Operations & Incidents', icon: AlertTriangle },
    { to: '/audit-logs', label: 'Audit Trail', icon: History },
  ];

  return (
    <header className="bg-slate-900/80 border-b border-slate-800 backdrop-blur sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-3">
            <div className="bg-emerald-500/10 p-2 rounded-lg border border-emerald-500/20 text-emerald-400">
              <Activity className="h-6 w-6" />
            </div>
            <div>
              <span className="font-bold text-lg text-white tracking-tight">Solatrace</span>
              <span className={`ml-2 text-xs font-medium px-2 py-0.5 rounded border inline-flex items-center gap-1 ${
                isConnected
                  ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                  : 'bg-amber-500/20 text-amber-400 border-amber-500/30'
              }`}>
                <Radio className="h-3 w-3 animate-pulse" />
                {isConnected ? 'LIVE WS' : 'POLLING'}
              </span>
            </div>
          </div>

          <nav className="flex space-x-1 sm:space-x-4">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                    }`
                  }
                >
                  <Icon className="h-4 w-4" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </div>
      </div>
    </header>
  );
};
