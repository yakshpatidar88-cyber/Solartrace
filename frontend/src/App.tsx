import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Dashboard } from './pages/Dashboard';
import { AssetsPage } from './pages/AssetsPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { AuditLogPage } from './pages/AuditLogPage';

export const App: React.FC = () => {
  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-[#0B1120] text-slate-100 selection:bg-emerald-500 selection:text-white">
        <Navbar />
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/assets" element={<AssetsPage />} />
            <Route path="/incidents" element={<IncidentsPage />} />
            <Route path="/audit-logs" element={<AuditLogPage />} />
          </Routes>
        </main>
        <footer className="border-t border-slate-900 bg-slate-950/40 py-6 text-center text-xs text-slate-500">
          GridOps Platform • Renewable Energy Asset Performance & Operations System
        </footer>
      </div>
    </Router>
  );
};
export default App;
